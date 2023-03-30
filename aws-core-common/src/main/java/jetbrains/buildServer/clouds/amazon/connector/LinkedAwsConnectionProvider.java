package jetbrains.buildServer.clouds.amazon.connector;

import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;

/**
 * Searches for the Connection which is linked to some ProjectFeature 
 */
public interface LinkedAwsConnectionProvider {
  @NotNull
  ConnectionCredentials getLinkedConnectionCredentials(@NotNull final SProjectFeatureDescriptor featureWithConnectionDescriptor) throws ConnectionCredentialsException;
}
