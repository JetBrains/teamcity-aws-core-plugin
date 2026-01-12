

package jetbrains.buildServer.serverSide.connections.aws.impl;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.ExtendableServiceLocator;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.ALLOWED_IN_SUBPROJECTS_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM;
import static jetbrains.buildServer.testUtils.TestUtils.getAwsCredentialsHolderCache;
import static jetbrains.buildServer.testUtils.TestUtils.getStsClientProviderWithNoKeys;
import static org.testng.Assert.assertTrue;

public class AwsConnectionCredentialsFactoryImplTest {

  private AwsConnectionCredentialsFactoryImpl myAwsConnectorFactory;
  private static final String MOCK_CREDENTIALS_BUILDER = "mockCredentialsBuilder";

  @BeforeMethod
  public void setup() {
    myAwsConnectorFactory = new AwsConnectionCredentialsFactoryImpl(Mockito.mock(ExtendableServiceLocator.class));
  }

  @Test
  public void givenAwsConn_WillReturnCredentials() throws ConnectionCredentialsException {
    AwsCredentialsBuilder credentialsBuilder = mockAwsCredentialsBuilder();


    ConnectionDescriptor descriptor = mockGetCredentialsType();
    mockConstructingCredentialsProvider(credentialsBuilder, descriptor);

    ConnectionCredentials credentials = myAwsConnectorFactory.requestCredentials(descriptor);
    Assert.assertNotNull(credentials);
  }

  @Test(expectedExceptions = ConnectionCredentialsException.class)
  public void givenAwsConn_WithWrongType_WillNotReturnCredentials() throws ConnectionCredentialsException {
    mockAwsCredentialsBuilder();
    ConnectionDescriptor descriptor = Mockito.mock(ConnectionDescriptor.class);
    Mockito.when(descriptor.getParameters()).thenReturn(Collections.singletonMap(CREDENTIALS_TYPE_PARAM, "fakeType"));
    myAwsConnectorFactory.requestCredentials(descriptor);
  }

  @Test(expectedExceptions = ConnectionCredentialsException.class)
  public void givenAwsConn_WithFailureInConstructingCredentialsProvider_WillNotReturnCredentials() throws ConnectionCredentialsException {
    AwsCredentialsBuilder credentialsBuilder = mockAwsCredentialsBuilder();


    ConnectionDescriptor descriptor = mockGetCredentialsType();
    AwsCredentialsHolder credentialsHolder = Mockito.mock(AwsCredentialsHolder.class);
    Mockito.when(credentialsHolder.getAwsCredentials()).thenReturn(Mockito.mock(AwsCredentialsData.class));
    Mockito.when(credentialsBuilder.constructSpecificCredentialsProvider(descriptor)).thenThrow(new AwsConnectorException("Mock error"));

    myAwsConnectorFactory.requestCredentials(descriptor);
  }

  @Test
  public void givenAwsConn_FromSubProject_WillReturnCredentials() throws ConnectionCredentialsException {
    AwsCredentialsBuilder credentialsBuilder = mockAwsCredentialsBuilder();


    ConnectionDescriptor descriptor = mockGetCredentialsType();
    Mockito.when(descriptor.getProjectId()).thenReturn("project1");
    mockConstructingCredentialsProvider(credentialsBuilder, descriptor);

    SProject project = Mockito.mock(SProject.class);
    Mockito.when(project.getProjectId()).thenReturn("project2");
    ConnectionCredentials credentials = myAwsConnectorFactory.requestCredentials(project, descriptor);
    Assert.assertNotNull(credentials);
  }

  @Test(expectedExceptions = ConnectionCredentialsException.class)
  public void givenAwsConn_FromSubProject_WithDisable_Availability_WillNotReturnCredentials() throws ConnectionCredentialsException {
    AwsCredentialsBuilder credentialsBuilder = mockAwsCredentialsBuilder();


    ConnectionDescriptor descriptor = Mockito.mock(ConnectionDescriptor.class);
    Mockito.when(descriptor.getParameters()).thenReturn(
      ImmutableMap.of(CREDENTIALS_TYPE_PARAM, MOCK_CREDENTIALS_BUILDER,
                      ALLOWED_IN_SUBPROJECTS_PARAM, "false"));
    Mockito.when(descriptor.getProjectId()).thenReturn("project1");
    mockConstructingCredentialsProvider(credentialsBuilder, descriptor);

    SProject project = Mockito.mock(SProject.class);
    Mockito.when(project.getProjectId()).thenReturn("project2");
    ConnectionCredentials credentials = myAwsConnectorFactory.requestCredentials(project, descriptor);
    Assert.assertNotNull(credentials);
  }

  private static void mockConstructingCredentialsProvider(AwsCredentialsBuilder credentialsBuilder, ConnectionDescriptor descriptor) throws ConnectionCredentialsException {
    AwsCredentialsHolder credentialsHolder = Mockito.mock(AwsCredentialsHolder.class);
    Mockito.when(credentialsHolder.getAwsCredentials()).thenReturn(Mockito.mock(AwsCredentialsData.class));
    Mockito.when(credentialsBuilder.constructSpecificCredentialsProvider(descriptor)).thenReturn(credentialsHolder);
  }

  @NotNull
  private static ConnectionDescriptor mockGetCredentialsType() {
    ConnectionDescriptor descriptor = Mockito.mock(ConnectionDescriptor.class);
    Mockito.when(descriptor.getParameters()).thenReturn(Collections.singletonMap(CREDENTIALS_TYPE_PARAM, MOCK_CREDENTIALS_BUILDER));
    return descriptor;
  }

  @NotNull
  private AwsCredentialsBuilder mockAwsCredentialsBuilder() {
    AwsCredentialsBuilder credentialsBuilder = Mockito.mock(AwsCredentialsBuilder.class);
    Mockito.when(credentialsBuilder.getCredentialsType()).thenReturn(MOCK_CREDENTIALS_BUILDER);
    myAwsConnectorFactory.registerAwsCredentialsBuilder(credentialsBuilder);
    return credentialsBuilder;
  }

  @Test
  public void givenAwsConnFactory_withUnknownCredentialsType_thenReturnInvalidPropsWithCredsTypeError() {
    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(
      Collections.singletonMap(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, "UNKNOWN")
    );

    assertTrue(invalidProperties.contains(
      new InvalidProperty(
        CREDENTIALS_TYPE_PARAM,
        "The credentials type " + "UNKNOWN" + " is not supported."
      )
    ));
  }

  @Test
  public void givenAwsConnFactory_withoutCredsTypeProp_thenReturnInvalidPropsWithCredsTypeError() {
    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(
      Collections.emptyMap()
    );

    assertTrue(invalidProperties.contains(
      new InvalidProperty(
        CREDENTIALS_TYPE_PARAM,
        "The credentials type " + "null" + " is not supported."
      )
    ));
  }

  @Test(expectedExceptions = {IllegalStateException.class})
  public void givenAwsConnBuilderWithRegisteredFactory_whenTryingToRegisterTheSameType_thenThrowException() throws ConnectionCredentialsException {
    new StaticCredentialsBuilder(Mockito.mock(AwsConnectorFactory.class), myAwsConnectorFactory, getStsClientProviderWithNoKeys(), getAwsCredentialsHolderCache());
    new StaticCredentialsBuilder(Mockito.mock(AwsConnectorFactory.class), myAwsConnectorFactory, getStsClientProviderWithNoKeys(), getAwsCredentialsHolderCache());
  }
}