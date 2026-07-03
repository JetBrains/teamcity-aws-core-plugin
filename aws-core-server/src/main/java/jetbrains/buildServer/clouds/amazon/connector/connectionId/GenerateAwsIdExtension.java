

package jetbrains.buildServer.clouds.amazon.connector.connectionId;

import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.controllers.admin.projects.GenerateExternalIdExtension;
import jetbrains.buildServer.serverSide.identifiers.ExternalIdGenerator;
import org.jetbrains.annotations.NotNull;

public class GenerateAwsIdExtension implements GenerateExternalIdExtension {

  private final AwsConnectionIdGenerator myAwsConnectionIdGenerator;

  public GenerateAwsIdExtension(@NotNull final AwsConnectionIdGenerator awsConnectionIdGenerator) {
    myAwsConnectionIdGenerator = awsConnectionIdGenerator;
  }

  @NotNull
  @Override
  public String getObjectId() {
    return AwsCloudConnectorConstants.AWS_CONNECTION_ID_GENERATOR_TYPE;
  }

  @NotNull
  @Override
  public ExternalIdGenerator getIdentifiersGenerator() {
    return myAwsConnectionIdGenerator;
  }
}