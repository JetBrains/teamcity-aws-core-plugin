package jetbrains.buildServer.clouds.amazon.connector.impl;

import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.AwsBuildFeatureException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.ChosenAwsConnPropertiesProcessor;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildLog.MessageAttrs;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.serverSide.connections.ProjectConnectionsManager;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    final boolean subProjectsFeatureEnabled = Boolean.parseBoolean(
      buildType.getConfigParameters().get(AwsCloudConnectorConstants.ALLOWED_IN_SUBPROJECTS_FEATURE_FLAG)
    );
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

    final boolean buildStepsFeatureEnabled = Boolean.parseBoolean(
      buildType.getConfigParameters().get(AwsCloudConnectorConstants.ALLOWED_IN_BUILDS_FEATURE_FLAG)
    );
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

    //TODO: TW-75618 Add support for several AWS Connections exposing - add a new property to each ConnectionCredentials with profile information
    for (SBuildFeatureDescriptor awsConnectionBuildFeature : awsConnectionsToInject) {
      return Collections.singletonList(getConnectionCredentials(project, awsConnectionBuildFeature.getParameters()));
    }

    return Collections.emptyList();
  }

  @NotNull
  private ConnectionCredentials getConnectionCredentials(@NotNull final SProject project, @NotNull final Map<String, String> featureProps) throws ConnectionCredentialsException {
    validateParamsWithLinkedConnectionId(featureProps);

    ConnectionDescriptor linkedAwsConnection = getLinkedConnectionFromParameters(project, featureProps);

    String sessionDuration = featureProps.get(SESSION_DURATION_PARAM);
    if (sessionDuration != null) {
      return myProjectConnectionCredentialsManager.requestConnectionCredentials(
        project,
        linkedAwsConnection.getId(),
        Collections.singletonMap(SESSION_DURATION_PARAM, sessionDuration)
      );
    } else {
      return myProjectConnectionCredentialsManager.requestConnectionCredentials(project, linkedAwsConnection.getId());
    }
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
}
