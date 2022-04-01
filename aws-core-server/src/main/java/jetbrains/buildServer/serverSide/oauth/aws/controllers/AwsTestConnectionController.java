package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.AwsConnectionTester;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.controllers.BasePropertiesBean;
import jetbrains.buildServer.controllers.admin.projects.PluginPropertiesUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jdom.Content;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;

public class AwsTestConnectionController extends BaseFormXmlController {
  public static final String PATH = TEST_CONNECTION_CONTROLLER_URL;

  private final AwsConnectionTester myAwsConnectionTester;

  public AwsTestConnectionController(@NotNull final SBuildServer server,
                                     @NotNull final WebControllerManager webControllerManager,
                                     @NotNull final AwsConnectionTester awsConnectionTester) {
    super(server);
    myAwsConnectionTester = awsConnectionTester;
    if (TeamCityProperties.getBoolean(FEATURE_PROPERTY_NAME)) {
      webControllerManager.registerController(PATH, this);
    }
  }

  @Override
  protected void doPost(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response, @NotNull final Element xmlResponse) {
    ActionErrors errors = new ActionErrors();

    BasePropertiesBean basePropertiesBean = new BasePropertiesBean(null);
    PluginPropertiesUtil.bindPropertiesFromRequest(request, basePropertiesBean);
    Map<String, String> connectionProperties = basePropertiesBean.getProperties();

    try {
      List<InvalidProperty> invalidProperties = myAwsConnectionTester.getInvalidProperties(connectionProperties);

      if (invalidProperties.isEmpty()) {
        GetCallerIdentityResult getCallerIdentityResult = myAwsConnectionTester.testConnection(connectionProperties);
        xmlResponse.addContent((Content)createCallerIdentityElement(getCallerIdentityResult));
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
  private Element createCallerIdentityElement(@NotNull final GetCallerIdentityResult getCallerIdentityResult) {
    Element callerIdentityElement = new Element(AWS_CALLER_IDENTITY_ELEMENT);
    callerIdentityElement.setAttribute(AWS_CALLER_IDENTITY_ATTR_ACCOUNT_ID, getCallerIdentityResult.getAccount());
    callerIdentityElement.setAttribute(AWS_CALLER_IDENTITY_ATTR_USER_ID, getCallerIdentityResult.getUserId());
    callerIdentityElement.setAttribute(AWS_CALLER_IDENTITY_ATTR_USER_ARN, getCallerIdentityResult.getArn());
    return callerIdentityElement;
  }

  private void handleException(@NotNull final Exception exception, @NotNull ActionErrors errors) {
    String actionDesription = "Unable to run AmazonSts.getCallerIdentity: ";
    Loggers.CLOUD.debug(actionDesription, exception);
    if (exception instanceof AmazonServiceException) {
      errors.addError(new InvalidProperty(CREDENTIALS_TYPE_PARAM, actionDesription + ((AmazonServiceException)exception).getErrorMessage()));
    } else {
      errors.addError(new InvalidProperty(CREDENTIALS_TYPE_PARAM, actionDesription + exception.getMessage()));
    }
  }

  @Override
  protected ModelAndView doGet(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
    return null;
  }
}