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

import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.AwsConnectionTester;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.serverSide.auth.*;
import jetbrains.buildServer.serverSide.impl.ProjectEx;
import jetbrains.buildServer.serverSide.oauth.aws.controllers.AwsTestConnectionController;
import jetbrains.buildServer.users.SUser;
import org.assertj.core.api.Assertions;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

public class AwsConnectionsTestConnectionRequestPermissionsCheckerTest extends BaseControllerTestCase<AwsTestConnectionController> {

  private SUser myUser;
  private ProjectEx myProject;

  @BeforeMethod
  protected void setUp() throws Exception {
    super.setUp();
    myProject = createProject("my_project");
    myUser = createUser("my_user");
  }

  @Override
  protected AwsTestConnectionController createController() throws IOException {
    return new AwsTestConnectionController(
      myServer,
      myWebManager,
      Mockito.mock(AwsConnectionTester.class),
      getWebFixture().getAuthorizationInterceptor(),
      myProjectManager,
      new AwsConnectionsRequestPermissionChecker()
      );
  }

  @Test
  void shouldThrowIfNoPermissions() throws Exception {
    makeLoggedIn(myUser);

    doPost("projectId", myProject.getExternalId());

    Assertions.assertThat(myResponse.getReturnedContent())
      .contains("Authorised user lacks permissions for the project: " + myProject.getExternalId());
  }

  @Test
  void shouldThrowIfNoProjectId() throws Exception {
    makeLoggedIn(myUser);

    doPost();

    Assertions.assertThat(myResponse.getReturnedContent())
      .contains("Authorised user lacks permissions for the project: unable to get from request");
  }

  @Test
  void shouldNotThrowForHappyPath() throws Exception {
    myUser.addRole(RoleScope.projectScope(myProject.getProjectId()), getTestRoles().createRole(Permission.EDIT_PROJECT));
    makeLoggedIn(myUser);

    doPost("projectId", myProject.getExternalId());
    Assertions.assertThat(myResponse.getReturnedContent())
      .doesNotContain("Authorised user lacks permissions for the project");
  }
}
