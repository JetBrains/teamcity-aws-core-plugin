

package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.AwsKeyRotator;
import jetbrains.buildServer.clouds.amazon.connector.utils.AwsExceptionUtils;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.controllers.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

public class AwsRotateKeysController extends BaseAwsConnectionController {
  private final AwsKeyRotator myAwsKeyRotator;
  private final ProjectManager myProjectManager;

  public AwsRotateKeysController(@NotNull final SBuildServer server,
                                 @NotNull final WebControllerManager webControllerManager,
                                 @NotNull final ProjectManager projectManager,
                                 @NotNull final AuthorizationInterceptor authInterceptor,
                                 @NotNull final AwsKeyRotator awsKeyRotator) {
    super(AwsAccessKeysParams.ROTATE_KEY_CONTROLLER_URL, server, projectManager, webControllerManager, authInterceptor);
    myAwsKeyRotator = awsKeyRotator;
    myProjectManager = projectManager;
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
    ActionErrors errors = new ActionErrors();
    try {
      final String projectId = request.getParameter("projectId");
      if (projectId == null) {
        throw new AwsConnectorException("The ID of the project where to rotate keys is null.");
      }
      SProject project = myProjectManager.findProjectByExternalId(projectId);
      if (project == null) {
        throw new AwsConnectorException("Could not find the project with id: " + projectId);
      }

      String connectionId = request.getParameter("connectionId");
      Loggers.CLOUD.debug("Starting the key rotation process...");
      myAwsKeyRotator.rotateConnectionKeys(connectionId, project);

    } catch (Exception e) {
      handleException(e, errors);
    }

    writeAsJson(errors, response);
    return null;
  }

  private void handleException(@NotNull final Exception exception, @NotNull ActionErrors errors) {
    String actionDescription = "Unable to rotate keys: ";
    Loggers.CLOUD.warnAndDebugDetails(actionDescription, exception);

    if (AwsExceptionUtils.isAmazonServiceException(exception)) {
      errors.addError(new InvalidProperty(AwsAccessKeysParams.ROTATE_KEY_BTTN_ID, actionDescription + exception.getMessage()));
    } else if(AwsExceptionUtils.isAmazonServiceException(exception.getCause())){
      errors.addError(new InvalidProperty(AwsAccessKeysParams.ROTATE_KEY_BTTN_ID, actionDescription + (exception.getCause()).getMessage()));
    } else {
      errors.addError(new InvalidProperty(AwsAccessKeysParams.ROTATE_KEY_BTTN_ID, actionDescription + exception.getMessage()));
    }
  }
}