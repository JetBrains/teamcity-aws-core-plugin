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

import jetbrains.buildServer.controllers.RequestPermissionsChecker;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.identifiers.ProjectIdentifiersManager;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class AwsConnectionsTestConnectionRequestPermissionsCheckerTest {

  private RequestPermissionsChecker myPermissionsChecker;
  private ProjectIdentifiersManager myProjectIdentifiersManager;
  private AuthorityHolder myAuthorityHolder;
  private HttpServletRequest myRequest;

  @BeforeMethod
  void setUp() throws IOException {
    myProjectIdentifiersManager = Mockito.mock(ProjectIdentifiersManager.class);
    myPermissionsChecker = new AwsConnectionsRequestPermissionChecker(myProjectIdentifiersManager);
    myAuthorityHolder = Mockito.mock(AuthorityHolder.class);
    myRequest = Mockito.mock(HttpServletRequest.class);
  }

  @Test
  void shouldThrowIfProjectIdIsNull() {
    Mockito.when(myAuthorityHolder.isPermissionGrantedForProject(SProject.ROOT_PROJECT_ID, Permission.EDIT_PROJECT))
      .thenReturn(false);

    Assert.assertThrows(AccessDeniedException.class, () -> myPermissionsChecker.checkPermissions(myAuthorityHolder, myRequest));
  }

  @Test
  void shouldThrowIfInternalProjectIdIsNull() {
    String externalProjectId = "123";
    Mockito.when(myAuthorityHolder.isPermissionGrantedForProject(SProject.ROOT_PROJECT_ID, Permission.EDIT_PROJECT))
      .thenReturn(false);
    Mockito.when(myProjectIdentifiersManager.externalToInternal(externalProjectId))
      .thenReturn(null);
    Mockito.when(myRequest.getParameter("projectId"))
      .thenReturn(externalProjectId);

    Assert.assertThrows(AccessDeniedException.class, () -> myPermissionsChecker.checkPermissions(myAuthorityHolder, myRequest));
  }

  @Test
  void shouldThrowIfNoPermissionsGranted() {
    String internalProjectId = "321";
    String externalProjectId = "123";
    Mockito.when(myAuthorityHolder.isPermissionGrantedForProject(internalProjectId, Permission.EDIT_PROJECT))
      .thenReturn(false);
    Mockito.when(myProjectIdentifiersManager.externalToInternal(externalProjectId))
      .thenReturn(internalProjectId);
    Mockito.when(myRequest.getParameter("projectId"))
      .thenReturn(externalProjectId);

    Assert.assertThrows(AccessDeniedException.class, () -> myPermissionsChecker.checkPermissions(myAuthorityHolder, myRequest));
  }

  @Test
  void shouldNotThrowForHappyPath() {
    String internalProjectId = "321";
    String externalProjectId = "123";
    Mockito.when(myAuthorityHolder.isPermissionGrantedForProject(internalProjectId, Permission.EDIT_PROJECT))
      .thenReturn(true);
    Mockito.when(myProjectIdentifiersManager.externalToInternal(externalProjectId))
      .thenReturn(internalProjectId);
    Mockito.when(myRequest.getParameter("projectId"))
      .thenReturn(externalProjectId);

    myPermissionsChecker.checkPermissions(myAuthorityHolder, myRequest);
  }
}
