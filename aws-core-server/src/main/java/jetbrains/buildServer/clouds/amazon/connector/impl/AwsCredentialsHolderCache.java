package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.intellij.openapi.util.Pair;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.utils.AwsConnectionUtils;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.impl.ProjectEx;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.sts.model.Credentials;

public class AwsCredentialsHolderCache {

  public static final String ENABLE_AWS_CREDENTIALS_CACHE = "teamcity.internal.aws.connection.credentialsCacheEnabled";
  public static final String CREDENTIALS_CACHE_EXPIRATION_BUFFER_SECONDS = "teamcity.internal.aws.connection.credentialsCache.expirationBufferInSeconds";
  private final ProjectManager myProjectManager;
  private final Cache<Pair<String, String>, Credentials> myCredentialsCache = CacheBuilder.newBuilder()
    .expireAfterWrite(Duration.ofHours(12))
    .build(); // Maximum session duration

  public AwsCredentialsHolderCache(@NotNull final EventDispatcher<BuildServerListener> eventDispatcher, ProjectManager projectManager) {
    myProjectManager = projectManager;
    eventDispatcher.addListener(new BuildServerAdapter() {
      @Override
      public void projectFeatureChanged(@NotNull SProject project, @NotNull SProjectFeatureDescriptor before, @NotNull SProjectFeatureDescriptor after) {
        myCredentialsCache.invalidate(Pair.create(before.getProjectId(), before.getId()));
      }

      @Override
      public void projectFeatureRemoved(@NotNull SProject project, @NotNull SProjectFeatureDescriptor projectFeature) {
        myCredentialsCache.invalidate(Pair.create(projectFeature.getProjectId(), projectFeature.getId()));
      }

      @Override
      public void projectRestored(@NotNull String projectId) {
        myCredentialsCache.asMap().keySet().forEach(key -> {
          if (key.getFirst().equals(projectId)) {
            myCredentialsCache.invalidate(key);
          }
        });
      }
    });
  }


  @NotNull
  public AwsCredentialsData getAwsCredentials(@NotNull SProjectFeatureDescriptor awsConnectionFeature, @NotNull RequestSessionFunction credentialsSupplier)
    throws ConnectionCredentialsException {
    final ProjectEx project = (ProjectEx)myProjectManager.findProjectById(awsConnectionFeature.getProjectId());
    final Credentials credentials;
    if (project == null || !project.getBooleanInternalParameterOrTrue(ENABLE_AWS_CREDENTIALS_CACHE)) {
      credentials = credentialsSupplier.get();
    } else {
      credentials = getOrRequestCredentials(awsConnectionFeature, credentialsSupplier);
    }

    return AwsConnectionUtils.getDataFromCredentials(credentials);
  }

  private Credentials getOrRequestCredentials(@NotNull SProjectFeatureDescriptor awsConnectionFeature, @NotNull RequestSessionFunction credentialsSupplier)
    throws ConnectionCredentialsException {
    final Credentials cachedCredentials = myCredentialsCache.getIfPresent(Pair.create(awsConnectionFeature.getProjectId(), awsConnectionFeature.getId()));
    if (cachedCredentials != null && isNotExpired(cachedCredentials)) {
      return cachedCredentials;
    } else {
      final Credentials credentials = credentialsSupplier.get();
      myCredentialsCache.put(Pair.create(awsConnectionFeature.getProjectId(), awsConnectionFeature.getId()), credentials);
      return credentials;
    }
  }

  private boolean isNotExpired(Credentials cachedCredentials) {
    // We add a configurable buffer amount to prevent passing credentials right about to expire
    final Instant currentInstance = Instant.now().plus(TeamCityProperties.getInteger(CREDENTIALS_CACHE_EXPIRATION_BUFFER_SECONDS, 1), ChronoUnit.SECONDS);
    return cachedCredentials.expiration().isAfter(
      currentInstance);
  }
}
