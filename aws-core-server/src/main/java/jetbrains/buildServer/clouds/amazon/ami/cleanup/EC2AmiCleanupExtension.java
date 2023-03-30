package jetbrains.buildServer.clouds.amazon.ami.cleanup;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.intellij.openapi.util.Pair;
import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.clouds.amazon.ami.AmiArtifact;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.EC2ClientCreator;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.cleanup.BuildCleanupContext;
import jetbrains.buildServer.serverSide.cleanup.BuildsCleanupExtension;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.ami.AmiConstants.AMI_CLEANUP_FEATURE_ENABLED;
import static jetbrains.buildServer.clouds.amazon.ami.AmiConstants.ARTIFACT_TYPE;
import static jetbrains.buildServer.log.Loggers.CLEANUP;

public class EC2AmiCleanupExtension implements BuildsCleanupExtension {

  public static final String AMIS_INFO_ERROR = "Cannot fetch information about AMIs";
  public static final String AMI_ERROR = "Cannot not delete AMI '%s'";
  public static final String SNAPSHOT_ERROR = "Cannot not delete snapshot '%s' for AMI '%s'";
  public static final String CONNECTION_ERROR = "Cannot find connection with id '%s'";
  public static final String EC2_CLIENT_ERROR = "Cannot use AWS EC2: %s";

  private final EC2ClientCreator myClientCreator;
  private volatile Map<Long, List<AmiArtifact>> myBuildAmiInfo;
  private final AwsConnectionsManager myConnectionsManager;

  public EC2AmiCleanupExtension(@NotNull AwsConnectionsManager connectionsManager, @NotNull EC2ClientCreator clientCreator) {
    myConnectionsManager = connectionsManager;
    myClientCreator = clientCreator;
  }

  @Override
  public void prepareBuildsData(@NotNull BuildCleanupContext cleanupContext) {
    if (!TeamCityProperties.getBoolean(AMI_CLEANUP_FEATURE_ENABLED)) {
      return;
    }
    myBuildAmiInfo = cleanupContext.getBuilds().stream().map(build -> {
      final List<AmiArtifact> artifacts = build.getRemoteArtifactsByType(ARTIFACT_TYPE).getArtifacts()
                                               .stream()
                                               .map(r -> new AmiArtifact(r.getAttributes()))
                                               .collect(Collectors.toList());
      return Pair.create(build.getBuildId(), artifacts);
    }).collect(Collectors.toMap(p -> p.getFirst(), p -> p.getSecond()));
  }

  @Override
  public void cleanupBuildsData(@NotNull BuildCleanupContext cleanupContext) {
    if (!TeamCityProperties.getBoolean(AMI_CLEANUP_FEATURE_ENABLED)) {
      return;
    }
    for (SFinishedBuild build : cleanupContext.getBuilds()) {
      final SBuildType buildType = build.getBuildType();
      if (buildType != null) {
        final List<AmiArtifact> remoteArtifacts = myBuildAmiInfo.get(build.getBuildId());

        final HashMap<String, AmazonEC2> clientsCache = new HashMap<>();

        final Map<String, List<AmiArtifact>> artifactsByConnection = remoteArtifacts.stream().collect(Collectors.groupingBy(AmiArtifact::getConnectionId));

        for (Map.Entry<String, List<AmiArtifact>> entry : artifactsByConnection.entrySet()) {
          cleanupArtifacts(build, buildType, entry.getKey(), entry.getValue(), clientsCache, cleanupContext);
        }
      }
    }
  }

  private void cleanupArtifacts(@NotNull SFinishedBuild build,
                                @NotNull SBuildType buildType,
                                @NotNull String connectionId,
                                @NotNull List<AmiArtifact> artifacts,
                                @NotNull HashMap<String, AmazonEC2> clientsCache,
                                @NotNull BuildCleanupContext cleanupContext) {
    final List<String> amiIds = artifacts.stream().map(a -> a.getAmiId()).collect(Collectors.toList());

    final AmazonEC2 client = getEC2Client(build, buildType.getProject(), connectionId, artifacts, cleanupContext, clientsCache);

    if (client != null) {
      List<Image> images = Collections.emptyList();
      try {
        images = client.describeImages(new DescribeImagesRequest().withImageIds(amiIds)).getImages();
      } catch (AmazonEC2Exception e) {
        CLEANUP.warnAndDebugDetails(AMIS_INFO_ERROR, e);
        cleanupContext.onBuildCleanupError(this, build, AMIS_INFO_ERROR);
      }
      for (Image image : images) {
        cleanupImage(cleanupContext, build, client, image);
      }
    }
  }

  @Nullable
  private AmazonEC2 getEC2Client(@NotNull SFinishedBuild build,
                                 @NotNull SProject project,
                                 @NotNull String connectionId,
                                 @NotNull List<AmiArtifact> artifacts,
                                 @NotNull BuildCleanupContext cleanupContext,
                                 @NotNull HashMap<String, AmazonEC2> clientsCache) {

    final Map<String, String> connectionAttributes = artifacts.isEmpty() ? Collections.emptyMap() : artifacts.get(0).getAttributes();

    return clientsCache.computeIfAbsent(connectionId, cid -> {
      final AwsConnectionBean awsConnection = myConnectionsManager.getAwsConnection(project, cid, connectionAttributes);

      if (awsConnection != null) {
        try {
          return myClientCreator.createClient(awsConnection);
        } catch (ConnectionCredentialsException e) {
          final String message = String.format(EC2_CLIENT_ERROR, e.getMessage());
          CLEANUP.warn(message);
          cleanupContext.onBuildCleanupError(this, build, message);
          return null;
        }
      } else {
        final String message = String.format(CONNECTION_ERROR, cid);
        CLEANUP.warn(message);
        cleanupContext.onBuildCleanupError(this, build, message);
        return null;
      }
    });
  }

  private void cleanupImage(@NotNull BuildCleanupContext cleanupContext, @NotNull SFinishedBuild build, @NotNull AmazonEC2 client, @NotNull Image image) {
    try {
      client.deregisterImage(new DeregisterImageRequest().withImageId(image.getImageId()));
      final List<String> snapshots = image.getBlockDeviceMappings().stream()
                                          .map(b -> b.getEbs())
                                          .filter(Objects::nonNull).map(ebs -> ebs.getSnapshotId())
                                          .collect(Collectors.toList());
      for (String snapshot : snapshots) {
        try {
          client.deleteSnapshot(new DeleteSnapshotRequest().withSnapshotId(snapshot));
        } catch (AmazonClientException e) {
          final String message = String.format(SNAPSHOT_ERROR, snapshot, image.getImageId());
          CLEANUP.warnAndDebugDetails(message, e);
          cleanupContext.onBuildCleanupError(this, build, message);
        }
      }
    } catch (AmazonClientException e) {
      final String message = String.format(AMI_ERROR, image.getImageId());
      CLEANUP.warnAndDebugDetails(message, e);
      cleanupContext.onBuildCleanupError(this, build, message);
    }
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "AWS EC2 AMI Cleanup";
  }
}
