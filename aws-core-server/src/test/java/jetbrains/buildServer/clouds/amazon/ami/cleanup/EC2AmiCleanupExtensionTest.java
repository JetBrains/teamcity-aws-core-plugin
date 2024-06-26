package jetbrains.buildServer.clouds.amazon.ami.cleanup;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import jetbrains.buildServer.clouds.amazon.ami.AmiArtifact;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.EC2ClientCreator;
import jetbrains.buildServer.serverSide.CleanupLevel;
import jetbrains.buildServer.serverSide.RunningBuildEx;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.cleanup.BuildCleanupContext;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.ami.AmiConstants.*;
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

    final AmazonEC2 ec2client = Mockito.mock(AmazonEC2.class, Answers.RETURNS_DEEP_STUBS);
    Mockito.when(clientCreator.createClient(any())).thenReturn(ec2client);

    final String snapshotId = "testSnapshot";
    final Image image = new Image()
      .withImageId(amiId)
      .withBlockDeviceMappings(new BlockDeviceMapping().withEbs(new EbsBlockDevice().withSnapshotId(snapshotId)));

    when(ec2client.describeImages(any(DescribeImagesRequest.class))).thenReturn(new DescribeImagesResult().withImages(image));

    final AwsConnectionsManager connectionsManager = Mockito.mock(AwsConnectionsManager.class);
    when(connectionsManager.getAwsConnection(any(), any(), any())).thenReturn(Mockito.mock(AwsConnectionBean.class));

    final EC2AmiCleanupExtension EC2AmiCleanupExtension = new EC2AmiCleanupExtension(connectionsManager, clientCreator);

    final BuildCleanupContext cleanupContext = getContextMock(Collections.singletonList(finishedBuild));

    EC2AmiCleanupExtension.prepareBuildsData(cleanupContext);
    EC2AmiCleanupExtension.cleanupBuildsData(cleanupContext);

    Mockito.verify(ec2client, times(1)).deregisterImage(new DeregisterImageRequest().withImageId(amiId));
    Mockito.verify(ec2client, times(1)).deleteSnapshot(new DeleteSnapshotRequest().withSnapshotId(snapshotId));
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
  public void logsWarningWhenFailsToFetchImageList() throws ConnectionCredentialsException {
    final RunningBuildEx runningBuild = startBuild();
    final HashMap<String, String> attributes = new HashMap<>();
    final String amiId = "testAmi";
    attributes.put(PARAM_AMI_ID, amiId);
    attributes.put(PARAM_CONNECTION_ID, "testConnection");
    runningBuild.addRemoteArtifact(new AmiArtifact(attributes));

    final SFinishedBuild finishedBuild = finishBuild();

    final EC2ClientCreator clientCreator = Mockito.mock(EC2ClientCreator.class, Answers.RETURNS_DEEP_STUBS);

    final AmazonEC2 ec2client = Mockito.mock(AmazonEC2.class, Answers.RETURNS_DEEP_STUBS);
    Mockito.when(clientCreator.createClient(any())).thenReturn(ec2client);

    when(ec2client.describeImages(any(DescribeImagesRequest.class))).thenThrow(new AmazonEC2Exception("test error"));

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

    final AmazonEC2 ec2client = Mockito.mock(AmazonEC2.class, Answers.RETURNS_DEEP_STUBS);
    Mockito.when(clientCreator.createClient(any())).thenReturn(ec2client);

    final String snapshotId = "testSnapshot";
    final Image image = new Image()
      .withImageId(amiId)
      .withBlockDeviceMappings(new BlockDeviceMapping().withEbs(new EbsBlockDevice().withSnapshotId(snapshotId)));

    when(ec2client.describeImages(any(DescribeImagesRequest.class))).thenReturn(new DescribeImagesResult().withImages(image));
    when(ec2client.deregisterImage(any())).thenThrow(new AmazonEC2Exception("failed to remove image"));

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

    final AmazonEC2 ec2client = Mockito.mock(AmazonEC2.class, Answers.RETURNS_DEEP_STUBS);
    Mockito.when(clientCreator.createClient(any())).thenReturn(ec2client);

    final String snapshotId = "testSnapshot";
    final Image image = new Image()
      .withImageId(amiId)
      .withBlockDeviceMappings(new BlockDeviceMapping().withEbs(new EbsBlockDevice().withSnapshotId(snapshotId)));

    when(ec2client.describeImages(any(DescribeImagesRequest.class))).thenReturn(new DescribeImagesResult().withImages(image));
    when(ec2client.deleteSnapshot(any())).thenThrow(new AmazonEC2Exception("failed to remove image"));

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
}