

package jetbrains.buildServer.clouds.amazon.connector.impl.iamRoleType;

import com.amazonaws.AmazonClientException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsExternalIdsManager;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.ChosenAwsConnPropertiesProcessor;
import jetbrains.buildServer.clouds.amazon.connector.impl.BaseAwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientProvider;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.StsEndpointParamValidator;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionCredentialsFactory;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.AwsExceptionUtils.getAwsErrorMessage;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil.*;

public class IamRoleCredentialsBuilder extends BaseAwsCredentialsBuilder {
  private final LinkedAwsConnectionProvider myLinkedConnectionProvider;
  private final AwsExternalIdsManager myAwsExternalIdsManager;
  private final StsClientProvider myStsClientProvider;

  public IamRoleCredentialsBuilder(@NotNull final AwsConnectorFactory awsConnectorFactory,
                                   @NotNull final AwsConnectionCredentialsFactory awsCredentialsFactory,
                                   @NotNull final LinkedAwsConnectionProvider linkedConnectionProvider,
                                   @NotNull final AwsExternalIdsManager awsExternalIdsManager,
                                   @NotNull final StsClientProvider stsClientProvider) {
    myLinkedConnectionProvider = linkedConnectionProvider;
    myStsClientProvider = stsClientProvider;
    awsConnectorFactory.registerAwsCredentialsBuilder(this);
    awsCredentialsFactory.registerAwsCredentialsBuilder(this);

    myAwsExternalIdsManager = awsExternalIdsManager;
  }

  @NotNull
  @Override
  protected AwsCredentialsHolder constructSpecificCredentialsProviderImpl(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException {
    try {
      return new IamRoleSessionCredentialsHolder(
        featureDescriptor,
        myLinkedConnectionProvider,
        myStsClientProvider,
        myAwsExternalIdsManager
      );
    } catch (AmazonClientException ace) {
      throw new AwsConnectorException("Failed to get the principal AWS connection to assume IAM Role: " + getAwsErrorMessage(ace));
    }
  }

  @Override
  @NotNull
  public List<InvalidProperty> validateProperties(@NotNull final Map<String, String> properties) {

    ChosenAwsConnPropertiesProcessor chosenAwsConnPropertiesProcessor = new ChosenAwsConnPropertiesProcessor();
    List<InvalidProperty> invalidProperties =
      new ArrayList<>(chosenAwsConnPropertiesProcessor.process(properties));

    String invalidArnReason = getInvalidArnReason(properties.get(IAM_ROLE_ARN_PARAM));
    if (invalidArnReason != null) {
      invalidProperties.add(new InvalidProperty(IAM_ROLE_ARN_PARAM, invalidArnReason));
    }

    if (!isValidSessionName(properties.get(IAM_ROLE_SESSION_NAME_PARAM))) {
      invalidProperties.add(
        new InvalidProperty(IAM_ROLE_SESSION_NAME_PARAM, "The Session Name is not valid, must satisfy regular expression pattern: " + VALID_ROLE_SESSION_NAME_REGEX));
    }

    if (!StsEndpointParamValidator.isValidStsEndpoint(properties.get(AwsAccessKeysParams.STS_ENDPOINT_PARAM))) {
      invalidProperties.add(
        new InvalidProperty(AwsAccessKeysParams.STS_ENDPOINT_PARAM, "The STS endpoint is not a valid URL, please, provide a valid URL"));
    }

    return invalidProperties;
  }

  @Override
  @NotNull
  public String getCredentialsType() {
    return AwsCloudConnectorConstants.IAM_ROLE_CREDENTIALS_TYPE;
  }

  @Override
  @NotNull
  public String getPropertiesDescription(@NotNull final Map<String, String> properties) {
    return
      "Assume " +
      getResourceNameFromArn(properties.get(IAM_ROLE_ARN_PARAM)) +
      " role to gain temporary credentials with specified privileges";
  }

  @NotNull
  @Override
  public Map<String, String> getDefaultProperties() {
    Map<String, String> defaultProperties = new HashMap<>();
    defaultProperties.put(IAM_ROLE_SESSION_NAME_PARAM, IAM_ROLE_SESSION_NAME_DEFAULT);
    defaultProperties.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, AwsCloudConnectorConstants.UNSELECTED_AWS_CONNECTION_ID_VALUE);
    return defaultProperties;
  }
}