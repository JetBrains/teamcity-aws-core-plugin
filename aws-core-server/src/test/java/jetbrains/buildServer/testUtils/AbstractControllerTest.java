

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