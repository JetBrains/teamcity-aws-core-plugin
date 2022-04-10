package jetbrains.buildServer.clouds.amazon.connector.buildFeatures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.util.StringUtil;

public class ChosenAwsConnPropertiesProcessor implements PropertiesProcessor {
  @Override
  public Collection<InvalidProperty> process(Map<String, String> properties) {
    ArrayList<InvalidProperty> invalidProperties = new ArrayList<>();
    if (StringUtil.nullIfEmpty(properties.get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM)) == null) {
      invalidProperties.add(new InvalidProperty(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, "AWS Connection was not specified"));
    }
    return invalidProperties;
  }
}
