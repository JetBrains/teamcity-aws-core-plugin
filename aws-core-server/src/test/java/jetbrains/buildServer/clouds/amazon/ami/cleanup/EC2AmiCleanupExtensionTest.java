package jetbrains.buildServer.clouds.amazon.ami.cleanup;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import jetbrains.buildServer.clouds.amazon.ami.AmiArtifact;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptorBuilder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionsHolder;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManagerImpl;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.EC2ClientCreator;
import jetbrains.buildServer.serverSide.CleanupLevel;
import jetbrains.buildServer.serverSide.RunningBuildEx;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.cleanup.BuildCleanupContext;
import jetbrains.buildServer.serverSide.connections.ConnectionProvider;
import jetbrains.buildServer.serverSide.connections.ProjectConnectionsManager;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import org.jetbrains.annotations.NotNull;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import static jetbrains.buildServer.clouds.amazon.ami.AmiConstants.*;
import static jetbrains.buildServer.clouds.amazon.ami.cleanup.EC2AmiCleanupExtension.CONNECTION_ERROR;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.ALLOWED_IN_BUILDS_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.ALLOWED_IN_SUBPROJECTS_PARAM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EC2AmiCleanupExtensionTest extends BaseServerTestCase {

  @BeforeMethod
  public void init() {
    setInternalProperty(AMI_CLEANUP_FEATURE_ENABLED, true);
  }

  @Test
  public void deregistersImages() throws ConnectionCredentialsException {
    final RunningBuildEx runningBuild = startBuild();
    final HashMap<String, String> attributes = new HashMap<>();
    final String amiId = "testAmi";
    attributes.put(PARAM_AMI_ID, amiId);
    attributes.put(PARAM_CONNECTION_ID, "testConnection");
    runningBuild.addRemoteArtifact(new AmiArtifact(attributes));

    final SFinishedBuild finishedBuild = finishBuild();

    final EC2ClientCreator clientCreator = Mockito.mock(EC2ClientCreator.class, Answers.RETURNS_DEEP_STUBS);

    final Ec2Client ec2client = Mockito.mock(Ec2Client.class, Answers.RETURNS_DEEP_STUBS);
    Mockito.when(clientCreator.createClient(any())).thenReturn(ec2client);

    final String snapshotId = "testSnapshot";
    final Image image = Image.builder()
      .imageId(amiId)
      .blockDeviceMappings(
        BlockDeviceMapping.builder()
          .ebs(
            EbsBlockDevice.builder()
              .snapshotId(snapshotId)
              .build()
          )
          .build()
      )
      .build();

    when(ec2client.describeImages(any(DescribeImagesRequest.class)))
      .thenReturn(DescribeImagesResponse.builder().images(image).build());

    final AwsConnectionsManager connectionsManager = Mockito.mock(AwsConnectionsManager.class);
    when(connectionsManager.getAwsConnection(any(), any(), any())).thenReturn(Mockito.mock(AwsConnectionBean.class));

    final EC2AmiCleanupExtension EC2AmiCleanupExtension = new EC2AmiCleanupExtension(connectionsManager, clientCreator);

    final BuildCleanupContext cleanupContext = getContextMock(Collections.singletonList(finishedBuild));

    EC2AmiCleanupExtension.prepareBuildsData(cleanupContext);
    EC2AmiCleanupExtension.cleanupBuildsData(cleanupContext);

    Mockito.verify(ec2client, times(1)).deregisterImage(DeregisterImageRequest.builder().imageId(amiId).build());
    Mockito.verify(ec2client, times(1)).deleteSnapshot(DeleteSnapshotRequest.builder().snapshotId(snapshotId).build());
  }

  @Test
  public void logsWarningWhenConnectionNotFound() {
    final RunningBuildEx runningBuild = startBuild();
    final HashMap<String, String> attributes = new HashMap<>();
    final String amiId = "testAmi";
    attributes.put(PARAM_AMI_ID, amiId);
    attributes.put(PARAM_CONNECTION_ID, "testConnection");
    runningBuild.addRemoteArtifact(new AmiArtifact(attributes));

    final SFinishedBuild finishedBuild = finishBuild();

    final EC2ClientCreator clientCreator = Mockito.mock(EC2ClientCreator.class, Answers.RETURNS_DEEP_STUBS);

    final AwsConnectionsManager connectionsManager = Mockito.mock(AwsConnectionsManager.class);
    when(connectionsManager.getAwsConnection(any(), any(), any())).thenReturn(null);

    final EC2AmiCleanupExtension EC2AmiCleanupExtension = new EC2AmiCleanupExtension(connectionsManager, clientCreator);

    final BuildCleanupContext cleanupContext = getContextMock(Collections.singletonList(finishedBuild));

    EC2AmiCleanupExtension.prepareBuildsData(cleanupContext);
    EC2AmiCleanupExtension.cleanupBuildsData(cleanupContext);

    Mockito.verify(cleanupContext, times(1)).onBuildCleanupError(any(), any(), any());
  }

  @Test
  public void deregistersImagesWithConnectionAvailableFromParentProject() throws Exception {
    final String connectionId = "parentConnection";
    final String amiId = "testAmi";
    final SFinishedBuild finishedBuild = finishBuildWithAmiArtifact(amiId, connectionId);
    final SProject buildProject = finishedBuild.getBuildType().getProject();

    final HashMap<String, String> attributes = new HashMap<>();
    attributes.put(ALLOWED_IN_SUBPROJECTS_PARAM, "true");
    attributes.put(ALLOWED_IN_BUILDS_PARAM, "true");

    final AwsConnectionDescriptor scopedConnection = createAwsConnectionDescriptor(connectionId, "parentProject", attributes);
    final ProjectConnectionsManager projectConnectionsManager = Mockito.mock(ProjectConnectionsManager.class);
    when(projectConnectionsManager.findConnectionById(buildProject, connectionId)).thenReturn(scopedConnection);

    final AwsConnectionsHolder connectionsHolder = Mockito.mock(AwsConnectionsHolder.class);
    when(connectionsHolder.getAwsConnection(connectionId)).thenReturn(scopedConnection);

    final AwsConnectionBean awsConnectionBean = Mockito.mock(AwsConnectionBean.class);
    final AwsConnectionDescriptorBuilder connectionDescriptorBuilder = Mockito.mock(AwsConnectionDescriptorBuilder.class);
    when(connectionDescriptorBuilder.awsConnBeanFromDescriptor(Mockito.eq(scopedConnection), any())).thenReturn(awsConnectionBean);

    final EC2ClientCreator clientCreator = Mockito.mock(EC2ClientCreator.class, Answers.RETURNS_DEEP_STUBS);
    final Ec2Client ec2client = Mockito.mock(Ec2Client.class, Answers.RETURNS_DEEP_STUBS);
    Mockito.when(clientCreator.createClient(awsConnectionBean)).thenReturn(ec2client);

    final String snapshotId = "testSnapshot";
    final Image image = Image.builder()
      .imageId(amiId)
      .blockDeviceMappings(
        BlockDeviceMapping
          .builder()
          .ebs(
            EbsBlockDevice
              .builder()
              .snapshotId(snapshotId)
              .build()
          )
          .build()
      )
      .build();

    when(ec2client.describeImages(any(DescribeImagesRequest.class)))
      .thenReturn(DescribeImagesResponse.builder().images(image).build());

    final EC2AmiCleanupExtension cleanupExtension = createCleanupExtensionWithAwsConnectionsManager(projectConnectionsManager, connectionsHolder, connectionDescriptorBuilder, clientCreator);
    final BuildCleanupContext cleanupContext = getContextMock(Collections.singletonList(finishedBuild));

    cleanupExtension.prepareBuildsData(cleanupContext);
    cleanupExtension.cleanupBuildsData(cleanupContext);

    Mockito.verify(ec2client, times(1)).deregisterImage(DeregisterImageRequest.builder().imageId(amiId).build());
    Mockito.verify(ec2client, times(1)).deleteSnapshot(DeleteSnapshotRequest.builder().snapshotId(snapshotId).build());
  }

  @Test
  public void doesNotDeregisterImagesWhenParentConnectionIsNotAvailableInSubprojects() throws Exception {
    final String connectionId = "parentConnection";
    final SFinishedBuild finishedBuild = finishBuildWithAmiArtifact("testAmi", connectionId);

    final HashMap<String, String> attributes = new HashMap<>();
    attributes.put(ALLOWED_IN_SUBPROJECTS_PARAM, "false");
    attributes.put(ALLOWED_IN_BUILDS_PARAM, "true");

    final AwsConnectionDescriptor scopedConnection = createAwsConnectionDescriptor(connectionId, "parentProject", attributes);
    final ProjectConnectionsManager projectConnectionsManager = Mockito.mock(ProjectConnectionsManager.class);
    when(projectConnectionsManager.findConnectionById(any(SProject.class), Mockito.eq(connectionId))).thenReturn(scopedConnection);

    final AwsConnectionsHolder connectionsHolder = Mockito.mock(AwsConnectionsHolder.class);
    final AwsConnectionDescriptorBuilder connectionDescriptorBuilder = Mockito.mock(AwsConnectionDescriptorBuilder.class);
    final EC2ClientCreator clientCreator = Mockito.mock(EC2ClientCreator.class, Answers.RETURNS_DEEP_STUBS);

    final EC2AmiCleanupExtension cleanupExtension = createCleanupExtensionWithAwsConnectionsManager(projectConnectionsManager, connectionsHolder, connectionDescriptorBuilder, clientCreator);
    final BuildCleanupContext cleanupContext = getContextMock(Collections.singletonList(finishedBuild));

    cleanupExtension.prepareBuildsData(cleanupContext);
    cleanupExtension.cleanupBuildsData(cleanupContext);

    Mockito.verify(connectionsHolder, Mockito.never()).getAwsConnection(any());
    Mockito.verify(connectionDescriptorBuilder, Mockito.never()).awsConnBeanFromDescriptor(any(), any());
    Mockito.verify(clientCreator, Mockito.never()).createClient(any());
    Mockito.verify(cleanupContext, times(1)).onBuildCleanupError(any(), Mockito.eq(finishedBuild), Mockito.eq(String.format(CONNECTION_ERROR, connectionId)));
  }

  @Test
  public void doesNotDeregisterImagesWhenConnectionIsNotAvailableForBuilds() throws Exception {
    final String connectionId = "sameProjectConnection";
    final SFinishedBuild finishedBuild = finishBuildWithAmiArtifact("testAmi", connectionId);

    final HashMap<String, String> attributes = new HashMap<>();
    attributes.put(ALLOWED_IN_BUILDS_PARAM, "false");

    final SProject buildProject = finishedBuild.getBuildType().getProject();
    final AwsConnectionDescriptor scopedConnection = createAwsConnectionDescriptor(connectionId, buildProject.getProjectId(), attributes);
    final ProjectConnectionsManager projectConnectionsManager = Mockito.mock(ProjectConnectionsManager.class);
    when(projectConnectionsManager.findConnectionById(buildProject, connectionId)).thenReturn(scopedConnection);

    final AwsConnectionsHolder connectionsHolder = Mockito.mock(AwsConnectionsHolder.class);
    final AwsConnectionDescriptorBuilder connectionDescriptorBuilder = Mockito.mock(AwsConnectionDescriptorBuilder.class);
    final EC2ClientCreator clientCreator = Mockito.mock(EC2ClientCreator.class, Answers.RETURNS_DEEP_STUBS);

    final EC2AmiCleanupExtension cleanupExtension = createCleanupExtensionWithAwsConnectionsManager(projectConnectionsManager, connectionsHolder, connectionDescriptorBuilder, clientCreator);
    final BuildCleanupContext cleanupContext = getContextMock(Collections.singletonList(finishedBuild));

    cleanupExtension.prepareBuildsData(cleanupContext);
    cleanupExtension.cleanupBuildsData(cleanupContext);

    Mockito.verify(connectionsHolder, Mockito.never()).getAwsConnection(any());
    Mockito.verify(connectionDescriptorBuilder, Mockito.never()).awsConnBeanFromDescriptor(any(), any());
    Mockito.verify(clientCreator, Mockito.never()).createClient(any());
    Mockito.verify(cleanupContext, times(1)).onBuildCleanupError(any(), Mockito.eq(finishedBuild), Mockito.eq(String.format(CONNECTION_ERROR, connectionId)));
  }

  @Test
  public void doesNotDeregisterImagesWhenConnectionIsOutsideBuildProjectPath() throws Exception {
    final String connectionId = "siblingConnection";
    final SFinishedBuild finishedBuild = finishBuildWithAmiArtifact("testAmi", connectionId);

    final ProjectConnectionsManager projectConnectionsManager = Mockito.mock(ProjectConnectionsManager.class);
    when(projectConnectionsManager.findConnectionById(any(SProject.class), Mockito.eq(connectionId))).thenReturn(null);

    final AwsConnectionsHolder connectionsHolder = Mockito.mock(AwsConnectionsHolder.class);
    final AwsConnectionDescriptorBuilder connectionDescriptorBuilder = Mockito.mock(AwsConnectionDescriptorBuilder.class);
    final EC2ClientCreator clientCreator = Mockito.mock(EC2ClientCreator.class, Answers.RETURNS_DEEP_STUBS);

    final EC2AmiCleanupExtension cleanupExtension = createCleanupExtensionWithAwsConnectionsManager(
      projectConnectionsManager,
      connectionsHolder,
      connectionDescriptorBuilder,
      clientCreator
    );

    final BuildCleanupContext cleanupContext = getContextMock(Collections.singletonList(finishedBuild));

    cleanupExtension.prepareBuildsData(cleanupContext);
    cleanupExtension.cleanupBuildsData(cleanupContext);

    Mockito.verify(connectionsHolder, Mockito.never()).getAwsConnection(any());
    Mockito.verify(connectionDescriptorBuilder, Mockito.never()).awsConnBeanFromDescriptor(any(), any());
    Mockito.verify(clientCreator, Mockito.never()).createClient(any());
    Mockito.verify(cleanupContext, times(1)).onBuildCleanupError(any(), Mockito.eq(finishedBuild), Mockito.eq(String.format(CONNECTION_ERROR, connectionId)));
  }

  @Test
  public void logsWarningWhenFailsToFetchImageList() throws ConnectionCredentialsException {
    final RunningBuildEx runningBuild = startBuild();
    final HashMap<String, String> attributes = new HashMap<>();
    final String amiId = "testAmi";
    attributes.put(PARAM_AMI_ID, amiId);
    attributes.put(PARAM_CONNECTION_ID, "testConnection");
    runningBuild.addRemoteArtifact(new AmiArtifact(attributes));

    final SFinishedBuild finishedBuild = finishBuild();

    final EC2ClientCreator clientCreator = Mockito.mock(EC2ClientCreator.class, Answers.RETURNS_DEEP_STUBS);

    final Ec2Client ec2client = Mockito.mock(Ec2Client.class, Answers.RETURNS_DEEP_STUBS);
    Mockito.when(clientCreator.createClient(any())).thenReturn(ec2client);

    when(ec2client.describeImages(any(DescribeImagesRequest.class))).thenThrow(AwsServiceException.create("test error", null));

    final AwsConnectionsManager connectionsManager = Mockito.mock(AwsConnectionsManager.class);
    when(connectionsManager.getAwsConnection(any(), any(), any())).thenReturn(Mockito.mock(AwsConnectionBean.class));

    final EC2AmiCleanupExtension EC2AmiCleanupExtension = new EC2AmiCleanupExtension(connectionsManager, clientCreator);

    final BuildCleanupContext cleanupContext = getContextMock(Collections.singletonList(finishedBuild));

    EC2AmiCleanupExtension.prepareBuildsData(cleanupContext);
    EC2AmiCleanupExtension.cleanupBuildsData(cleanupContext);

    Mockito.verify(cleanupContext, times(1)).onBuildCleanupError(any(), any(), any());
  }

  @Test
  public void logsWarningWhenFailsToRemoveImage() throws ConnectionCredentialsException {
    final RunningBuildEx runningBuild = startBuild();
    final HashMap<String, String> attributes = new HashMap<>();
    final String amiId = "testAmi";
    attributes.put(PARAM_AMI_ID, amiId);
    attributes.put(PARAM_CONNECTION_ID, "testConnection");
    runningBuild.addRemoteArtifact(new AmiArtifact(attributes));

    final SFinishedBuild finishedBuild = finishBuild();

    final EC2ClientCreator clientCreator = Mockito.mock(EC2ClientCreator.class, Answers.RETURNS_DEEP_STUBS);

    final Ec2Client ec2client = Mockito.mock(Ec2Client.class, Answers.RETURNS_DEEP_STUBS);
    Mockito.when(clientCreator.createClient(any())).thenReturn(ec2client);

    final String snapshotId = "testSnapshot";
    final Image image = Image.builder()
      .imageId(amiId)
      .blockDeviceMappings(
        BlockDeviceMapping.builder()
          .ebs(
            EbsBlockDevice.builder()
              .snapshotId(snapshotId)
              .build()
          )
          .build()
      )
      .build();

    when(ec2client.describeImages(any(DescribeImagesRequest.class)))
      .thenReturn(DescribeImagesResponse.builder().images(image).build());
    when(ec2client.deregisterImage(any(DeregisterImageRequest.class)))
      .thenThrow(AwsServiceException.create("failed to remove image", null));

    final AwsConnectionsManager connectionsManager = Mockito.mock(AwsConnectionsManager.class);
    when(connectionsManager.getAwsConnection(any(), any(), any())).thenReturn(Mockito.mock(AwsConnectionBean.class));

    final EC2AmiCleanupExtension EC2AmiCleanupExtension = new EC2AmiCleanupExtension(connectionsManager, clientCreator);

    final BuildCleanupContext cleanupContext = getContextMock(Collections.singletonList(finishedBuild));

    EC2AmiCleanupExtension.prepareBuildsData(cleanupContext);
    EC2AmiCleanupExtension.cleanupBuildsData(cleanupContext);

    Mockito.verify(cleanupContext, times(1)).onBuildCleanupError(any(), any(), any());
  }

  @Test
  public void logsWarningWhenFailsToRemoveSnapshot() throws ConnectionCredentialsException {
    final RunningBuildEx runningBuild = startBuild();
    final HashMap<String, String> attributes = new HashMap<>();
    final String amiId = "testAmi";
    attributes.put(PARAM_AMI_ID, amiId);
    attributes.put(PARAM_CONNECTION_ID, "testConnection");
    runningBuild.addRemoteArtifact(new AmiArtifact(attributes));

    final SFinishedBuild finishedBuild = finishBuild();

    final EC2ClientCreator clientCreator = Mockito.mock(EC2ClientCreator.class, Answers.RETURNS_DEEP_STUBS);

    final Ec2Client ec2client = Mockito.mock(Ec2Client.class, Answers.RETURNS_DEEP_STUBS);
    Mockito.when(clientCreator.createClient(any())).thenReturn(ec2client);

    final String snapshotId = "testSnapshot";
    final Image image = Image.builder()
      .imageId(amiId)
      .blockDeviceMappings(
        BlockDeviceMapping.builder()
          .ebs(
            EbsBlockDevice.builder()
              .snapshotId(snapshotId)
              .build()
          )
          .build()
      )
      .build();

    when(ec2client.describeImages(any(DescribeImagesRequest.class)))
      .thenReturn(DescribeImagesResponse.builder().images(image).build());
    when(ec2client.deregisterImage(any(DeregisterImageRequest.class)))
      .thenThrow(AwsServiceException.create("failed to remove image", null));

    final AwsConnectionsManager connectionsManager = Mockito.mock(AwsConnectionsManager.class);
    when(connectionsManager.getAwsConnection(any(), any(), any())).thenReturn(Mockito.mock(AwsConnectionBean.class));

    final EC2AmiCleanupExtension cleanupExtension = new EC2AmiCleanupExtension(connectionsManager, clientCreator);

    final BuildCleanupContext cleanupContext = getContextMock(Collections.singletonList(finishedBuild));

    cleanupExtension.prepareBuildsData(cleanupContext);
    cleanupExtension.cleanupBuildsData(cleanupContext);

    Mockito.verify(cleanupContext, times(1)).onBuildCleanupError(any(), any(), any());
  }

  private BuildCleanupContext getContextMock(List<SFinishedBuild> builds) {
    final ConcurrentHashMap<String, Object> extensionDataMap = new ConcurrentHashMap<>();
    final BuildCleanupContext cleanupContext = Mockito.mock(BuildCleanupContext.class);
    doReturn(CleanupLevel.HISTORY_ENTRY).when(cleanupContext).getCleanupLevel();
    doReturn(builds).when(cleanupContext).getBuilds();
    doAnswer(invocation -> {
      String key = invocation.getArgument(0);
      return extensionDataMap.get(key);
    }).when(cleanupContext).getExtensionData(any());
    doAnswer(invocation -> {
      String key = invocation.getArgument(0);
      Object data = invocation.getArgument(1);
      extensionDataMap.put(key, data);
      return null;
    }).when(cleanupContext).setExtensionData(any(), any());
    return cleanupContext;
  }

  private SFinishedBuild finishBuildWithAmiArtifact(String amiId, String connectionId) {
    final RunningBuildEx runningBuild = startBuild();
    final HashMap<String, String> attributes = new HashMap<>();
    attributes.put(PARAM_AMI_ID, amiId);
    attributes.put(PARAM_CONNECTION_ID, connectionId);
    runningBuild.addRemoteArtifact(new AmiArtifact(attributes));
    return finishBuild();
  }

  private EC2AmiCleanupExtension createCleanupExtensionWithAwsConnectionsManager(@NotNull ProjectConnectionsManager projectConnectionsManager,
                                                                                 @NotNull AwsConnectionsHolder connectionsHolder,
                                                                                 @NotNull AwsConnectionDescriptorBuilder connectionDescriptorBuilder,
                                                                                 @NotNull EC2ClientCreator clientCreator) {
    return new EC2AmiCleanupExtension(
      new AwsConnectionsManagerImpl(connectionsHolder, connectionDescriptorBuilder, projectConnectionsManager),
      clientCreator
    );
  }

  private static AwsConnectionDescriptor createAwsConnectionDescriptor(@NotNull String connectionId,
                                                                      @NotNull String projectId,
                                                                      @NotNull HashMap<String, String> parameters) {
    final ConnectionProvider connectionProvider = Mockito.mock(ConnectionProvider.class);
    when(connectionProvider.getType()).thenReturn(AwsConnectionProvider.TYPE);

    final AwsConnectionDescriptor connectionDescriptor = Mockito.mock(AwsConnectionDescriptor.class);
    when(connectionDescriptor.getId()).thenReturn(connectionId);
    when(connectionDescriptor.getProjectId()).thenReturn(projectId);
    when(connectionDescriptor.getParameters()).thenReturn(parameters);
    when(connectionDescriptor.getConnectionProvider()).thenReturn(connectionProvider);
    return connectionDescriptor;
  }
}
