package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.SESSION_DURATION_ERROR;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams.AWS_PROFILE_ERROR;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams.AWS_PROFILE_NAME_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.SESSION_DURATION_PARAM;

public class ChosenAwsConnPropertiesProcessor implements PropertiesProcessor {
  @Override
  public Collection<InvalidProperty> process(Map<String, String> properties) {
    ArrayList<InvalidProperty> invalidProperties = new ArrayList<>();

    String chosenAwsConnectionId = properties.get(CHOSEN_AWS_CONN_ID_PARAM);
    if (chosenAwsConnectionId == null ||
        AwsCloudConnectorConstants.UNSELECTED_AWS_CONNECTION_ID_VALUE.equals(chosenAwsConnectionId)) {
      invalidProperties.add(new InvalidProperty(CHOSEN_AWS_CONN_ID_PARAM, "Linked AWS Connection ID is not specified"));
    }

    if (! ParamUtil.isValidSessionDuration(properties.get(SESSION_DURATION_PARAM))) {
      invalidProperties.add(new InvalidProperty(SESSION_DURATION_PARAM, SESSION_DURATION_ERROR));
    }

    if (! ParamUtil.isValidAwsProfileName(properties.get(AWS_PROFILE_NAME_PARAM))) {
      invalidProperties.add(new InvalidProperty(AWS_PROFILE_NAME_PARAM, AWS_PROFILE_ERROR));
    }
    return invalidProperties;
  }
}
