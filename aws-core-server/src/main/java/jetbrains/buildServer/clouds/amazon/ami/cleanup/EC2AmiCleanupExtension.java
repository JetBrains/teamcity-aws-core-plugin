package jetbrains.buildServer.clouds.amazon.ami.cleanup;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DeleteSnapshotRequest;
import software.amazon.awssdk.services.ec2.model.DeregisterImageRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.Image;

import static jetbrains.buildServer.clouds.amazon.ami.AmiConstants.AMI_CLEANUP_FEATURE_ENABLED;
import static jetbrains.buildServer.clouds.amazon.ami.AmiConstants.ARTIFACT_TYPE;
import static jetbrains.buildServer.log.Loggers.CLEANUP;

public class EC2AmiCleanupExtension implements BuildsCleanupExtension {

  public static final String AMIS_INFO_ERROR = "Cannot fetch information about AMIs";
  public static final String AMI_ERROR = "Cannot not delete AMI '%s'";
  public static final String SNAPSHOT_ERROR = "Cannot not delete snapshot '%s' for AMI '%s'";
  public static final String CONNECTION_ERROR = "Cannot find connection with id '%s'";
  public static final String EC2_CLIENT_ERROR = "Cannot use AWS EC2: %s";
  private static final String BUILD_AMI_INFO_KEY = EC2AmiCleanupExtension.class.getName() + ".BUILD_AMI_INFO";

  private final EC2ClientCreator myClientCreator;
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
    Map<Long, List<AmiArtifact>> buildAmiInfo = new HashMap<Long, List<AmiArtifact>>();
    if (cleanupContext.getCleanupLevel().isCleanHistoryEntry()) {
      for (SFinishedBuild build : cleanupContext.getBuilds()) {
        final List<AmiArtifact> artifacts = build.getRemoteArtifactsByType(ARTIFACT_TYPE).getArtifacts()
                                                 .stream()
                                                 .map(r -> new AmiArtifact(r.getAttributes()))
                                                 .collect(Collectors.toList());
        buildAmiInfo.put(build.getBuildId(), artifacts);
      }
    }
    cleanupContext.setExtensionData(BUILD_AMI_INFO_KEY, buildAmiInfo);
  }

  @Override
  public void cleanupBuildsData(@NotNull BuildCleanupContext cleanupContext) {
    if (!TeamCityProperties.getBoolean(AMI_CLEANUP_FEATURE_ENABLED)) {
      return;
    }
    if (!cleanupContext.getCleanupLevel().isCleanHistoryEntry()) {
      return;
    }
    //noinspection unchecked
    Map<Long, List<AmiArtifact>> buildAmiInfo = (Map<Long, List<AmiArtifact>>)cleanupContext.getExtensionData(BUILD_AMI_INFO_KEY);
    if (buildAmiInfo == null) {
      throw new IllegalStateException("Extension data should have been initialized during `prepareBuildsData` stage.");
    }
    if (buildAmiInfo.isEmpty()) {
      return;
    }
    for (SFinishedBuild build : cleanupContext.getBuilds()) {
      final SBuildType buildType = build.getBuildType();
      if (buildType != null) {
        final List<AmiArtifact> remoteArtifacts = buildAmiInfo.get(build.getBuildId());

        final HashMap<String, Ec2Client> clientsCache = new HashMap<>();

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
                                @NotNull HashMap<String, Ec2Client> clientsCache,
                                @NotNull BuildCleanupContext cleanupContext) {
    final List<String> amiIds = artifacts.stream()
      .map(AmiArtifact::getAmiId)
      .collect(Collectors.toList());

    final Ec2Client client = getEC2Client(build, buildType.getProject(), connectionId, artifacts, cleanupContext, clientsCache);

    if (client != null) {
      List<Image> images = Collections.emptyList();
      try {
        images = client.describeImages(
            DescribeImagesRequest.builder()
              .imageIds(amiIds)
              .build())
          .images();
      } catch (SdkException e) {
        CLEANUP.warnAndDebugDetails(AMIS_INFO_ERROR, e);
        cleanupContext.onBuildCleanupError(this, build, AMIS_INFO_ERROR);
      }
      for (Image image : images) {
        cleanupImage(cleanupContext, build, client, image);
      }
    }
  }

  @Nullable
  private Ec2Client getEC2Client(@NotNull SFinishedBuild build,
                                 @NotNull SProject project,
                                 @NotNull String connectionId,
                                 @NotNull List<AmiArtifact> artifacts,
                                 @NotNull BuildCleanupContext cleanupContext,
                                 @NotNull HashMap<String, Ec2Client> clientsCache) {

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

  private void cleanupImage(@NotNull BuildCleanupContext cleanupContext, @NotNull SFinishedBuild build, @NotNull Ec2Client client, @NotNull Image image) {
    try {
      client.deregisterImage(DeregisterImageRequest.builder()
        .imageId(image.imageId())
        .build());
      final List<String> snapshots = image.blockDeviceMappings().stream()
                                          .map(BlockDeviceMapping::ebs)
                                          .filter(Objects::nonNull).map(EbsBlockDevice::snapshotId)
                                          .collect(Collectors.toList());
      for (String snapshot : snapshots) {
        try {
          client.deleteSnapshot(DeleteSnapshotRequest.builder()
            .snapshotId(snapshot)
            .build());
        } catch (SdkException e) {
          final String message = String.format(SNAPSHOT_ERROR, snapshot, image.imageId());
          CLEANUP.warnAndDebugDetails(message, e);
          cleanupContext.onBuildCleanupError(this, build, message);
        }
      }
    } catch (SdkException e) {
      final String message = String.format(AMI_ERROR, image.imageId());
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
