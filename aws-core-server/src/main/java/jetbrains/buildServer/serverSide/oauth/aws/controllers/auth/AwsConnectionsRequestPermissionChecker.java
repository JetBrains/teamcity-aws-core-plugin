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
import jetbrains.buildServer.controllers.RequestPermissionsChecker;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.identifiers.ProjectIdentifiersManager;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;

public class AwsConnectionsRequestPermissionChecker implements RequestPermissionsChecker {

  private static final Logger LOG = Logger.getInstance(AwsConnectionsRequestPermissionChecker.class);

  private final ProjectIdentifiersManager myProjectIdentifiersManager;

  public AwsConnectionsRequestPermissionChecker(@NotNull final ProjectIdentifiersManager projectIdentifiersManager) {
    myProjectIdentifiersManager = projectIdentifiersManager;
  }

  @Override
  public void checkPermissions(@NotNull AuthorityHolder holder, @NotNull HttpServletRequest request) throws AccessDeniedException {

    String projectId = request.getParameter("projectId");

    if (projectId != null) {
      projectId = myProjectIdentifiersManager.externalToInternal(projectId);
    }

    if (projectId == null) {
      LOG.debug("Resulting project ID is calculated as null while checking permissions");
      projectId = SProject.ROOT_PROJECT_ID;
    }

    final boolean hasAccess = holder.isPermissionGrantedForProject(projectId, Permission.EDIT_PROJECT);

    if (!hasAccess) {
      throw new AccessDeniedException(holder, "Authorised user lacks permissions for project " + projectId);
    }
  }
}
