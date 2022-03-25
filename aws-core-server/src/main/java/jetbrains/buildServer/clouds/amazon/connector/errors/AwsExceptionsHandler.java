package jetbrains.buildServer.clouds.amazon.connector.errors;

import com.amazonaws.AmazonClientException;
import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.util.text.StringUtil.notNullize;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM;
import static jetbrains.buildServer.util.StringUtil.nullIfEmpty;

public class AwsExceptionsHandler {
  @NotNull
  public static String getAwsErrorDescription(@NotNull final AmazonClientException amazonException) {
    return StringUtil.split(amazonException.getMessage(), "(").get(0);
  }

  public static void addAwsConnectorException(@NotNull final AwsConnectorException awsConnectorException, @NotNull ActionErrors errors) {
    String parameterName = notNullize(nullIfEmpty(awsConnectorException.getParameterName()), CREDENTIALS_TYPE_PARAM);
    errors.addError(new InvalidProperty(parameterName, awsConnectorException.getMessage()));
  }
}