package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.AwsConnectionTester;
import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.impl.AwsTestConnectionResult;
import jetbrains.buildServer.clouds.amazon.connector.utils.AwsExceptionUtils;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.controllers.BasePropertiesBean;
import jetbrains.buildServer.controllers.admin.projects.PluginPropertiesUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.serverSide.oauth.aws.controllers.auth.AwsConnectionsRequestPermissionChecker;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jdom.Content;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;

public class AwsTestConnectionController extends BaseFormXmlController {
  public static final String PATH = TEST_CONNECTION_CONTROLLER_URL;

  private final AwsConnectionTester myAwsConnectionTester;
  private final ProjectManager myProjectManager;
  private final AwsConnectionsRequestPermissionChecker myPermissionChecker;

  public AwsTestConnectionController(@NotNull final SBuildServer server,
                                     @NotNull final WebControllerManager webControllerManager,
                                     @NotNull final AwsConnectionTester awsConnectionTester,
                                     @NotNull final AuthorizationInterceptor authInterceptor,
                                     @NotNull final ProjectManager projectManager,
                                     @NotNull final AwsConnectionsRequestPermissionChecker permissionChecker) {
    super(server);
    myAwsConnectionTester = awsConnectionTester;
    myProjectManager = projectManager;
    if (TeamCityProperties.getBooleanOrTrue(FEATURE_PROPERTY_NAME)) {
      webControllerManager.registerController(PATH, this);
    }
    myPermissionChecker = permissionChecker;
  }

  @Override
  protected void doPost(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response, @NotNull final Element xmlResponse) {
    Loggers.CLOUD.debug("AWS Connection testing has been requested.");

    String externalProjectId = request.getParameter("projectId");
    String internalProjectId = "";
    SProject project = myProjectManager.findProjectByExternalId(externalProjectId);
    if (project != null) {
      internalProjectId = project.getProjectId();
    }

    myPermissionChecker.checkUserPermission(internalProjectId, externalProjectId, request);

    ActionErrors errors = new ActionErrors();

    BasePropertiesBean basePropertiesBean = new BasePropertiesBean(null);
    PluginPropertiesUtil.bindPropertiesFromRequest(request, basePropertiesBean);
    Map<String, String> connectionProperties = basePropertiesBean.getProperties();

    try {
      List<InvalidProperty> invalidProperties = myAwsConnectionTester.getInvalidProperties(connectionProperties);

      if (invalidProperties.isEmpty()) {
        AwsTestConnectionResult testConnectionResult = myAwsConnectionTester.testConnection(
          new ProjectFeatureDescriptorImpl(
            StringUtil.emptyIfNull(request.getParameter("connectionId")),
            AwsConnectionProvider.TYPE,
            connectionProperties,
            internalProjectId
          )
        );
        GetCallerIdentityResponse getCallerIdentityResult = testConnectionResult.getGetCallerIdentityResult();
        xmlResponse.addContent(createCallerIdentityElement(getCallerIdentityResult));
      } else {
        for (InvalidProperty invalidProp : invalidProperties) {
          errors.addError(invalidProp);
        }
      }
    } catch (Exception e) {
      handleException(e, errors);
    }

    if (errors.hasErrors()) {
      errors.serialize(xmlResponse);
    }
  }

  @NotNull
  private Element createCallerIdentityElement(@NotNull final GetCallerIdentityResponse getCallerIdentityResult) {
    Element callerIdentityElement = new Element(AWS_CALLER_IDENTITY_ELEMENT);
    callerIdentityElement.setAttribute(AWS_CALLER_IDENTITY_ATTR_ACCOUNT_ID, getCallerIdentityResult.account());
    callerIdentityElement.setAttribute(AWS_CALLER_IDENTITY_ATTR_USER_ID, getCallerIdentityResult.userId());
    callerIdentityElement.setAttribute(AWS_CALLER_IDENTITY_ATTR_USER_ARN, getCallerIdentityResult.arn());
    return callerIdentityElement;
  }

  private void handleException(@NotNull final Exception exception, @NotNull ActionErrors errors) {
    String actionDescription = "Unable to run AmazonSts.getCallerIdentity: ";

    if(AwsExceptionUtils.isAmazonServiceException(exception) || AwsExceptionUtils.isAmazonServiceException(exception.getCause())) {
      errors.addError(new InvalidProperty(CREDENTIALS_TYPE_PARAM, actionDescription + AwsExceptionUtils.getAwsErrorMessage(exception)));
      Loggers.CLOUD.debug(actionDescription, exception);
    } else if(exception instanceof ConnectionCredentialsException) {
      errors.addError(new InvalidProperty(CREDENTIALS_TYPE_PARAM, actionDescription + exception.getMessage()));
      Loggers.CLOUD.debug(actionDescription, exception);
    } else {
      String unrelatedToAwsExcaptionMessage = " Got exception which is unrelated to AWS STS, please, make sure that your call hits correct endpoint";
      errors.addError(new InvalidProperty(CREDENTIALS_TYPE_PARAM, actionDescription + unrelatedToAwsExcaptionMessage));
      Loggers.CLOUD.debug(actionDescription + unrelatedToAwsExcaptionMessage, exception);
    }
  }

  @Override
  protected ModelAndView doGet(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
    return null;
  }
}
