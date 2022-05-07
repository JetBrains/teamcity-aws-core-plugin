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

package jetbrains.buildServer.testUtils;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.ProjectManager;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import static org.mockito.Mockito.when;

public abstract class AbstractControllerTest extends BaseTestCase {
  protected final String PROJECT_ID = "PROJECT_ID";

  protected HttpServletRequest request;
  protected HttpServletResponse response;
  protected ByteArrayOutputStream responseOutputStream;
  protected OutputStreamWriter responseStreamWriter;

  protected ProjectManager projectManager;

  public void setUp() throws IOException {
    request = Mockito.mock(HttpServletRequest.class);
    when(request.getParameter("projectId"))
      .thenReturn(PROJECT_ID);

    response = Mockito.mock(HttpServletResponse.class);
    responseOutputStream = new ByteArrayOutputStream();
    responseStreamWriter = new OutputStreamWriter(responseOutputStream);
    when(response.getWriter()).thenReturn(new PrintWriter(responseStreamWriter));


    projectManager = Mockito.mock(ProjectManager.class);
  }
}
