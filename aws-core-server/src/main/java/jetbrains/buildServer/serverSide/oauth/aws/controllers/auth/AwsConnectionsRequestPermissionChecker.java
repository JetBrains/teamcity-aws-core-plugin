/*
 * Copyright 2000-2025 JetBrains s.r.o.
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

package jetbrains.buildServer.serverSide.oauth.aws.controllers.auth;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.util.SessionUser;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;

public class AwsConnectionsRequestPermissionChecker {

  private static final Logger LOG = Logger.getInstance(AwsConnectionsRequestPermissionChecker.class);

  public void checkUserPermission(@Nullable String internalProjectId,
                                  @Nullable String externalProjectId,
                                  @NotNull HttpServletRequest request) {
    internalProjectId = getProjectId(internalProjectId);
    externalProjectId = StringUtils.isEmpty(externalProjectId) ? "unable to get from request" : externalProjectId;
    SUser user = getUserFromRequest(request);

    boolean hasAccess = user.isPermissionGrantedForProject(internalProjectId, Permission.EDIT_PROJECT);

    if (!hasAccess) {
      throw new AccessDeniedException(user, "Authorised user lacks permissions for the project: " + externalProjectId);
    }
  }

  private SUser getUserFromRequest(@NotNull HttpServletRequest request) {
    return SessionUser.getUser(request);
  }

  @NotNull
  private String getProjectId(@Nullable String projectId) {
    if (StringUtils.isEmpty(projectId)) {
      LOG.debug("Project ID is not set, resolving project ID to a Root Project ID");
      projectId = SProject.ROOT_PROJECT_ID;
    }

    return projectId;
  }
}
