package jetbrains.buildServer.clouds.amazon.connector.impl;

import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import software.amazon.awssdk.services.sts.model.Credentials;

@FunctionalInterface
public interface RequestSessionFunction {
  Credentials get() throws ConnectionCredentialsException;
}
