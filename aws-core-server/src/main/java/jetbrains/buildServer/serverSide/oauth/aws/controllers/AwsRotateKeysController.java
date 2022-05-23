/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import com.amazonaws.AmazonServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.AwsKeyRotator;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.controllers.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME;

public class AwsRotateKeysController extends BaseController {
  private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final AwsKeyRotator myAwsKeyRotator;
  private final ProjectManager myProjectManager;

  public AwsRotateKeysController(@NotNull final SBuildServer server,
                                 @NotNull final WebControllerManager webControllerManager,
                                 @NotNull final ProjectManager projectManager,
                                 @NotNull final AuthorizationInterceptor authInterceptor,
                                 @NotNull final AwsKeyRotator awsKeyRotator) {
    super(server);
    myAwsKeyRotator = awsKeyRotator;
    myProjectManager = projectManager;

    if (TeamCityProperties.getBoolean(FEATURE_PROPERTY_NAME)) {
      final RequestPermissionsChecker projectAccessChecker = (RequestPermissionsCheckerEx)(securityContext, request) -> {
        String projectId = request.getParameter("projectId");
        SProject curProject = myProjectManager.findProjectByExternalId(projectId);
        if (curProject == null) {
          throw new AccessDeniedException(securityContext.getAuthorityHolder(), "Project with id " + request.getParameter("projectId") + " does not exist");
        }
        securityContext.getAccessChecker().checkCanEditProject(curProject);
      };

      webControllerManager.registerController(AwsAccessKeysParams.ROTATE_KEY_CONTROLLER_URL, this);
      authInterceptor.addPathBasedPermissionsChecker(AwsAccessKeysParams.ROTATE_KEY_CONTROLLER_URL, projectAccessChecker);
    }
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
      myAwsKeyRotator.rotateConnectionKeys(connectionId, project);

    } catch (Exception e) {
      handleException(e, errors);
    }

    writeAsJson(errors, response);
    return null;
  }

  private void handleException(@NotNull final Exception exception, @NotNull ActionErrors errors) {
    String actionDesription = "Unable to rotate keys: ";
    Loggers.CLOUD.debug(actionDesription, exception);
    if (exception instanceof AmazonServiceException) {
      errors.addError(new InvalidProperty(AwsAccessKeysParams.ROTATE_KEY_BTTN_ID, actionDesription + ((AmazonServiceException)exception).getErrorMessage()));
    } else {
      errors.addError(new InvalidProperty(AwsAccessKeysParams.ROTATE_KEY_BTTN_ID, actionDesription + exception.getMessage()));
    }
  }

  private <T> void writeAsJson(@NotNull T value, @NotNull HttpServletResponse response) throws IOException {
    final String json = OBJECT_MAPPER.writeValueAsString(value);
    response.setContentType("application/json");
    response.setCharacterEncoding(Charsets.UTF_8.name());
    final PrintWriter writer = response.getWriter();
    writer.write(json);
    writer.flush();
  }
}