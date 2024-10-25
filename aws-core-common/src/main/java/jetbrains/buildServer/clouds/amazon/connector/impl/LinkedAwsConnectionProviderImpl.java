package jetbrains.buildServer.clouds.amazon.connector.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.amazonaws.auth.AWSCredentialsProvider;
import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.AwsBuildFeatureException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.ChosenAwsConnPropertiesProcessor;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnectionParameters;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.BuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.buildLog.MessageAttrs;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.serverSide.connections.ProjectConnectionsManager;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.SESSION_DURATION_PARAM;

/**
 * Register this as a bean in your plugin to manage linked AWS Connection searching
 *
 * @since 2023.11
 */
  public class LinkedAwsConnectionProviderImpl implements LinkedAwsConnectionProvider {

  private final ProjectManager myProjectManager;
  private final ProjectConnectionsManager myProjectConnectionsManager;
  private final ProjectConnectionCredentialsManager myProjectConnectionCredentialsManager;

  public LinkedAwsConnectionProviderImpl(@NotNull final ProjectManager projectManager,
                                         @NotNull final ProjectConnectionsManager projectConnectionsManager,
                                         @NotNull final ProjectConnectionCredentialsManager projectConnectionCredentialsManager) {
    myProjectManager = projectManager;
    myProjectConnectionsManager = projectConnectionsManager;
    myProjectConnectionCredentialsManager = projectConnectionCredentialsManager;
  }

  @NotNull
  @Override
  public ConnectionCredentials getLinkedConnectionCredentials(@NotNull final SProjectFeatureDescriptor featureWithConnectionDescriptor) throws ConnectionCredentialsException {
    String failedMessage =
      String.format("Failed to get linked AWS connection for project feature <%s> in Project <%s>. Reason: ", featureWithConnectionDescriptor.getId(), featureWithConnectionDescriptor.getProjectId());

    SProject project = myProjectManager.findProjectById(featureWithConnectionDescriptor.getProjectId());
    if (project == null) {
      throw new AwsConnectorException(failedMessage + "Cannot find the Project with ID: " + featureWithConnectionDescriptor.getProjectId());
    }

    Map<String, String> featureParams = featureWithConnectionDescriptor.getParameters();

    try {
      return getConnectionCredentials(project, featureParams);

    } catch (ConnectionCredentialsException e) {
      throw new AwsConnectorException(failedMessage + e.getMessage());
    }
  }

  @NotNull
  @Override
  public ConnectionCredentials getCredentialsFromParameters(@NotNull SProject project, @NotNull final BuildRunnerDescriptor buildRunnerWithChosenConnection)
    throws ConnectionCredentialsException {
    String failedMessage =
      String.format("Failed to get linked AWS connection for build runner <%s> in Project <%s>. Reason: ", buildRunnerWithChosenConnection.getId(), project.getExternalId());

    Map<String, String> parameters = buildRunnerWithChosenConnection.getParameters();

    try {
      return getConnectionCredentials(project, parameters);

    } catch (ConnectionCredentialsException e) {
      throw new AwsConnectorException(failedMessage + e.getMessage());
    }
  }

  @NotNull
  @Override
  public ConnectionDescriptor getLinkedConnectionFromParameters(@NotNull final SProject project, @NotNull final Map<String, String> featureProperties) throws ConnectionCredentialsException {
    validateParamsWithLinkedConnectionId(featureProperties);

    String linkedAwsConnId = ParamUtil.getLinkedAwsConnectionId(featureProperties);
    if (linkedAwsConnId == null) {
      throw new AwsConnectorException(String.format("There is no AWS Connection ID property: <%s> in the feature properties", CHOSEN_AWS_CONN_ID_PARAM));
    }
    ConnectionDescriptor awsConnection = myProjectConnectionsManager.findConnectionById(project, linkedAwsConnId);
    if (awsConnection == null) {
      throw new AwsConnectorException(String.format("Cannot find the linked AWS Connection with id: <%s> in the Project <%s>: ", linkedAwsConnId, project.getExternalId()));
    }
    return awsConnection;
  }

  @NotNull
  @Override
  public AWSCredentialsProvider getAwsCredentialsProvider(@NotNull AwsConnectionParameters awsConnectionParameters) throws ConnectionCredentialsException {
    SProject project = getProjectFromAwsConnectionParameters(awsConnectionParameters);
    return getAwsCredentialsProvider(project, awsConnectionParameters);
  }

  @NotNull
  @Override
  public List<ConnectionCredentials> getConnectionCredentialsFromBuild(@NotNull SBuild build) throws ConnectionCredentialsException {
    SBuildType buildType = build.getBuildType();
    if (buildType == null) {
      throw new AwsBuildFeatureException("There is no BuildType for the Build with id: " + build.getBuildId());
    }

    String failedMessage =
      String.format("Failed to inject AWS connection for Build with ID: <%s>, BuildConfiguration: <%s> in Project <%s>: ", build.getBuildId(), buildType.getExternalId(), build.getProjectId());

    SProject project = myProjectManager.findProjectById(buildType.getProjectId());
    if (project == null) {
      throw new AwsBuildFeatureException(failedMessage + "Cannot find this Project");
    }

    Collection<SBuildFeatureDescriptor> awsConnectionsToInject = build.getBuildFeaturesOfType(AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE);
    if (awsConnectionsToInject.isEmpty()) {
      return Collections.emptyList();
    }

    final boolean subProjectsFeatureEnabled = ParamUtil.toBooleanOrTrue(buildType, AwsCloudConnectorConstants.ALLOWED_IN_SUBPROJECTS_FEATURE_FLAG);
    if (subProjectsFeatureEnabled) {
      reportInfo(build, "Filtering AWS Connections, injecting connections only from the same project or allowed to be used in SubProjects");

      awsConnectionsToInject = awsConnectionsToInject
        .stream()
        .filter(
          awsConnBuildFeature -> {
            try {
              ConnectionDescriptor awsConnectionToInject = getLinkedConnectionFromParameters(project, awsConnBuildFeature.getParameters());
              return
                awsConnectionToInject.getProjectId().equals(buildType.getProjectId()) ||
                ParamUtil.isAllowedInSubProjects(awsConnectionToInject.getParameters());

            } catch (ConnectionCredentialsException e) {
              Loggers.CLOUD.warn(String.format("Project %s, Build %s, AWS Connection will not be injected: %s", project.getExternalId(), build.getBuildId(), e.getMessage()));
            }
            return false;
          }
        )
        .collect(Collectors.toList());
    }

    final boolean buildStepsFeatureEnabled = ParamUtil.toBooleanOrTrue(buildType, AwsCloudConnectorConstants.ALLOWED_IN_BUILDS_FEATURE_FLAG);
    if (buildStepsFeatureEnabled) {
      reportInfo(build, "Filtering AWS Connections, injecting connections which are allowed to be used in Builds");

      awsConnectionsToInject = awsConnectionsToInject
        .stream()
        .filter(
          awsConnBuildFeature -> {
            try {
              ConnectionDescriptor awsConnectionToInject = getLinkedConnectionFromParameters(project, awsConnBuildFeature.getParameters());
              final String isAllowedInBuildSteps = awsConnectionToInject.getParameters().get(AwsCloudConnectorConstants.ALLOWED_IN_BUILDS_PARAM);
              return isAllowedInBuildSteps == null || Boolean.parseBoolean(isAllowedInBuildSteps);
            } catch (ConnectionCredentialsException e) {
              Loggers.CLOUD.warn(String.format("Project %s, Build %s, AWS Connection will not be injected: %s", project.getExternalId(), build.getBuildId(), e.getMessage()));
            }
            return false;
          }
        )
        .collect(Collectors.toList());
    }

    List<ConnectionCredentials> credentialsToInject = new ArrayList<>();
    for (SBuildFeatureDescriptor awsConnectionBuildFeature : awsConnectionsToInject) {
      credentialsToInject.add(getConnectionCredentials(project, awsConnectionBuildFeature.getParameters()));
    }

    return credentialsToInject;
  }

  @NotNull
  private ConnectionCredentials getConnectionCredentials(@NotNull final SProject project, @NotNull final Map<String, String> featureProps) throws ConnectionCredentialsException {
    validateParamsWithLinkedConnectionId(featureProps);

    ConnectionDescriptor linkedAwsConnection = getLinkedConnectionFromParameters(project, featureProps);

    String sessionDuration = featureProps.get(SESSION_DURATION_PARAM);
    Map<String, String> additionalProperties = new HashMap<>();
    if (sessionDuration != null) {
      additionalProperties.put(SESSION_DURATION_PARAM, sessionDuration);
    }
    ConnectionCredentials connectionCredentials = myProjectConnectionCredentialsManager.requestConnectionCredentials(
      project,
      linkedAwsConnection.getId(),
      additionalProperties
    );

    //TW-77603 Add AWS Profile name for multiple AWS Connection injection
    String awsProfileName = featureProps.get(AwsConnBuildFeatureParams.AWS_PROFILE_NAME_PARAM);
    if (awsProfileName != null) {
      return createCredentialsWithAwsProfileName(connectionCredentials, awsProfileName);
    }
    return connectionCredentials;
  }

  private void validateParamsWithLinkedConnectionId(@NotNull final Map<String, String> featureProperties) throws ConnectionCredentialsException {
    ChosenAwsConnPropertiesProcessor awsConnsPropertiesProcessor = new ChosenAwsConnPropertiesProcessor();
    Collection<InvalidProperty> invalidProps = awsConnsPropertiesProcessor.process(featureProperties);
    if (!invalidProps.isEmpty()) {
      InvalidProperty invalidProperty = invalidProps.iterator().next();
      throw new ConnectionCredentialsException(
        String.format("Invalid linked AWS Connection property: %s, reason: %s", invalidProperty.getPropertyName(), invalidProperty.getInvalidReason())
      );
    }
  }

  private void reportInfo(SBuild build, String message) {
    build.getBuildLog().messageAsync(
      message,
      Status.NORMAL,
      MessageAttrs.fromMessage(DefaultMessagesInfo.createTextMessage(message))
    );
  }

  @NotNull
  private AWSCredentialsProvider getAwsCredentialsProvider(@NotNull final SProject project,
                                                           @NotNull AwsConnectionParameters awsConnectionParameters) throws ConnectionCredentialsException {
    String connectionId = awsConnectionParameters.getAwsConnectionId();

    ConnectionCredentials connectionCredentials = getConnectionCredentials(project,
      new HashMap<String, String>(){{
        put(CHOSEN_AWS_CONN_ID_PARAM, connectionId);
        put(SESSION_DURATION_PARAM, awsConnectionParameters.getSessionDuration());
      }});

    if (!(connectionCredentials instanceof AwsConnectionCredentials)) {
      throw new AwsConnectorException("Invalid linked AWS connection: This connection is not supported, " +
        "project ID = [" + project.getProjectId() + "], connection ID = [" + connectionId + "]");
    }

    return ((AwsConnectionCredentials) connectionCredentials).toAWSCredentialsProvider();
  }

  @NotNull
  private SProject getProjectFromAwsConnectionParameters(@NotNull AwsConnectionParameters awsConnectionParameters) throws AwsConnectorException {
    SProject sProject = myProjectManager.findProjectById(awsConnectionParameters.getInternalProjectId());

    if (sProject == null) {
      throw new AwsConnectorException("Cannot find the Project with ID = [" + awsConnectionParameters.getInternalProjectId() + "]");
    }

    return sProject;
  }

  private ConnectionCredentials createCredentialsWithAwsProfileName(@NotNull final ConnectionCredentials connectionCredentials, @NotNull final String awsProfileName) {
    Map<String, String> propsWithAwsProfileName = new HashMap<>(connectionCredentials.getProperties());
    propsWithAwsProfileName.put(AwsConnBuildFeatureParams.AWS_PROFILE_NAME_PARAM, awsProfileName);

    return new ConnectionCredentials() {
      @NotNull
      @Override
      public Map<String, String> getProperties() {
        return propsWithAwsProfileName;
      }

      @NotNull
      @Override
      public String getProviderType() {
        return connectionCredentials.getProviderType();
      }
    };
  }
}
