package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.util.StringUtil;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.SESSION_DURATION_ERROR;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.SESSION_DURATION_PARAM;

public class ChosenAwsConnPropertiesProcessor implements PropertiesProcessor {
  @Override
  public Collection<InvalidProperty> process(Map<String, String> properties) {
    ArrayList<InvalidProperty> invalidProperties = new ArrayList<>();
    if (StringUtil.nullIfEmpty(properties.get(CHOSEN_AWS_CONN_ID_PARAM)) == null) {
      invalidProperties.add(new InvalidProperty(CHOSEN_AWS_CONN_ID_PARAM, "AWS Connection was not specified"));
    }
    if (! ParamUtil.isValidSessionDuration(properties.get(SESSION_DURATION_PARAM))) {
      invalidProperties.add(new InvalidProperty(SESSION_DURATION_PARAM, SESSION_DURATION_ERROR));
    }
    return invalidProperties;
  }
}
