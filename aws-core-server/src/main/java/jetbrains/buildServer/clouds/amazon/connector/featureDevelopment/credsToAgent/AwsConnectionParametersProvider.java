package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.parameters.AbstractBuildParametersProvider;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionParametersProvider extends AbstractBuildParametersProvider {

  @NotNull
  @Override
  public Collection<String> getParametersAvailableOnAgent(@NotNull SBuild build) {
    if (!AwsConnToAgentBuildFeature.getAwsConnectionsToExpose(build).isEmpty()) {
      ArrayList<String> params = new ArrayList<>();
      params.add(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_PROFILE_NAME_ENV);
      params.add(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SHARED_CREDENTIALS_FILE_ENV);
      return params;
    }
    return Collections.emptyList();
  }
}
