package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.connections.ConnectionProvider;
import jetbrains.buildServer.serverSide.connections.ProjectConnectionsManager;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

public class SupportedProvidersController extends BaseAwsConnectionController {
  public static final String CONTROLLER_PATH = "/app/connections/aws/supportedProviders.html";
  private final ProjectConnectionsManager myConnectionsManager;

  public SupportedProvidersController(@NotNull SBuildServer server,
                                      @NotNull ProjectConnectionsManager connectionsManager,
                                      @NotNull ProjectManager projectManager,
                                      @NotNull WebControllerManager webControllerManager,
                                      @NotNull AuthorizationInterceptor authInterceptor) {
    super(CONTROLLER_PATH, server, projectManager, webControllerManager, authInterceptor);
    myConnectionsManager = connectionsManager;
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
    if ("GET".equals(request.getMethod())) {
      doGet(response);
    } else {
      response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
    }

    return null;
  }

  private void doGet(HttpServletResponse response) throws IOException {
    Collection<ConnectionProvider> providers = myConnectionsManager.getConnectionProviders();
    Map<String, String> supportedProviders =
      providers.stream()
               .filter(ConnectionProvider::isAvailable)
               .sorted(Comparator.comparing(ConnectionProvider::getDisplayOrderRank).thenComparing(ConnectionProvider::getDisplayName))
               .collect(Collectors.toMap(
                 ConnectionProvider::getType,
                 ConnectionProvider::getDisplayName,
                 (oldV, newV) -> oldV,
                 LinkedHashMap::new
               ));
    writeAsJson(supportedProviders, response);
  }
}
