package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsExceptionsHandler;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientBuilder;
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

  private final AwsConnectorFactory myAwsConnectorFactory;

  public AwsTestConnectionController(@NotNull final SBuildServer server,
                                     @NotNull final WebControllerManager webControllerManager,
                                     @NotNull final AwsConnectorFactory awsConnectorFactory) {
    super(server);
    myAwsConnectorFactory = awsConnectorFactory;
    if (TeamCityProperties.getBoolean(FEATURE_PROPERTY_NAME)) {
      webControllerManager.registerController(PATH, this);
    }
  }

  @Override
  protected void doPost(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response, @NotNull final Element xmlResponse) {
    ActionErrors errors = new ActionErrors();

    BasePropertiesBean basePropertiesBean = new BasePropertiesBean(null);
    PluginPropertiesUtil.bindPropertiesFromRequest(request, basePropertiesBean);

    try {
      List<InvalidProperty> invalidProperties = myAwsConnectorFactory.validateProperties(basePropertiesBean.getProperties());
      if (invalidProperties.isEmpty()) {
        GetCallerIdentityRequest getCallerIdentityRequest = new GetCallerIdentityRequest()
          .withRequestCredentialsProvider(
            myAwsConnectorFactory.buildAwsCredentialsProvider(basePropertiesBean.getProperties())
          );

        AWSSecurityTokenServiceClientBuilder stsClientBuilder = AWSSecurityTokenServiceClientBuilder.standard();
        StsClientBuilder.addConfiguration(stsClientBuilder, basePropertiesBean.getProperties());
        AWSSecurityTokenService sts = stsClientBuilder.build();

        GetCallerIdentityResult getCallerIdentityResult = sts.getCallerIdentity(getCallerIdentityRequest);
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

  private Element createCallerIdentityElement(GetCallerIdentityResult getCallerIdentityResult) {
    Element callerIdentityElement = new Element(AWS_CALLER_IDENTITY_ELEMENT);
    callerIdentityElement.setAttribute(AWS_CALLER_IDENTITY_ATTR_ACCOUNT_ID, getCallerIdentityResult.getAccount());
    callerIdentityElement.setAttribute(AWS_CALLER_IDENTITY_ATTR_USER_ID, getCallerIdentityResult.getUserId());
    callerIdentityElement.setAttribute(AWS_CALLER_IDENTITY_ATTR_USER_ARN, getCallerIdentityResult.getArn());
    return callerIdentityElement;
  }

  private void handleException(Exception e, ActionErrors errors) {
    String actionDesription = "Unable to run AmazonSts.getCallerIdentity. ";

    if (e instanceof AwsConnectorException) {
      Loggers.CLOUD.debug("Failed to create the AWS Connector: " + e.getMessage());
      AwsExceptionsHandler.addAwsConnectorException((AwsConnectorException)e, errors);
    } else if (e instanceof AmazonClientException) {
      AmazonClientException amazonException = (AmazonClientException)e;
      Loggers.CLOUD.debug(actionDesription + "(got an AWS exception)", amazonException);
      String errorDescription = AwsExceptionsHandler.getAwsErrorDescription(amazonException);
      errors.addError(new InvalidProperty(CREDENTIALS_TYPE_PARAM, actionDesription + errorDescription));
    } else {
      Loggers.CLOUD.debug(actionDesription, e);
      errors.addError(new InvalidProperty(CREDENTIALS_TYPE_PARAM, actionDesription + e.getMessage()));
    }
  }

  @Override
  protected ModelAndView doGet(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
    return null;
  }
}