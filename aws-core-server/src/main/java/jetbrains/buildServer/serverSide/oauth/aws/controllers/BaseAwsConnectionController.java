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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.oauth.ProjectAccessChecker;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME;

public abstract class BaseAwsConnectionController extends BaseController {

  private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public BaseAwsConnectionController(@NotNull final String controllerPath,
                                     @NotNull final SBuildServer server,
                                     @NotNull final ProjectManager projectManager,
                                     @NotNull final WebControllerManager webControllerManager,
                                     @NotNull final AuthorizationInterceptor authInterceptor){
    super(server);
    if (TeamCityProperties.getBooleanOrTrue(FEATURE_PROPERTY_NAME)) {
      webControllerManager.registerController(controllerPath, this);
      authInterceptor.addPathBasedPermissionsChecker(controllerPath, new ProjectAccessChecker(projectManager));
    }
  }

  protected <T> void writeAsJson(@NotNull T value, @NotNull HttpServletResponse response) throws IOException {
    final String json = OBJECT_MAPPER.writeValueAsString(value);
    response.setContentType("application/json");
    response.setCharacterEncoding(Charsets.UTF_8.name());
    final PrintWriter writer = response.getWriter();
    writer.write(json);
    writer.flush();
  }
}
