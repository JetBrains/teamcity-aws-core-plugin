package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment;

import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.LinkedAwsConnNotFoundException;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AwsConnectionsManager {
  /**
   * Will get AWS Connection ID from the properties, find corresponding AWS Connection
   * and return a data bean with all properties like connectionId, providerType and all properties map.
   *
   * @param properties properties Map where should be a parameter with chosen AWS Connection ID.
   * @param project    project which will be searched for the AWS Connection.
   * @return AwsConnectionBean data bean with all AWS Connection properties.
   * @throws LinkedAwsConnNotFoundException thrown when there is no corresponding {@link AwsCloudConnectorConstants#CHOSEN_AWS_CONN_ID_PARAM property} in the properties map.
   */
  @Nullable
  public AwsConnectionBean getLinkedAwsConnection(@NotNull final Map<String, String> properties, @NotNull final SProject project) throws LinkedAwsConnNotFoundException;

  @Nullable
  public AwsConnectionBean getAwsConnectionForBuild(@NotNull final SBuild build);
}
