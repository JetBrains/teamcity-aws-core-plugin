/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.amazon.connector.connectionId;

import com.intellij.openapi.diagnostic.Logger;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.serverSide.oauth.identifiers.OAuthConnectionsIdGenerator;
import jetbrains.buildServer.util.CachingTypedIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.USER_DEFINED_ID_PARAM;

public class AwsConnectionIdGenerator implements CachingTypedIdGenerator {
  public final static String ID_GENERATOR_TYPE = AwsConnectionProvider.TYPE;
  public final static String AWS_CONNECTION_ID_PREFIX = "awsConnection";
  public final static int INITIAL_AWS_CONNECTION_ID = 0;

  private final static Logger LOG = Logger.getInstance(AwsConnectionIdGenerator.class.getName());

  private final ConcurrentHashMap<String, String> awsConnectionIdxMap = new ConcurrentHashMap<>();

  public AwsConnectionIdGenerator(@NotNull final OAuthConnectionsIdGenerator OAuthConnectionsIdGenerator) {
    OAuthConnectionsIdGenerator.registerProviderTypeGenerator(ID_GENERATOR_TYPE, this);
  }

  public static String formatId(@NotNull final String connectionId, int newIdNumber) {
    return String.format("%s_%s", connectionId, String.valueOf(newIdNumber));
  }

  @Nullable
  @Override
  public String newId(@NotNull Map<String, String> props) {
    String userDefinedConnId = props.get(USER_DEFINED_ID_PARAM);
    props.remove(USER_DEFINED_ID_PARAM, userDefinedConnId);

    if (userDefinedConnId == null) {
      userDefinedConnId = generateNewId();
      LOG.debug("User did not define the connection id, will generate it using awsConnection_<incremental ID>");
    } else if (!isUnique(userDefinedConnId)) {
      userDefinedConnId = makeUnique(userDefinedConnId);
      LOG.warn("User-defined connection id is not unique, will add incremental ID");
    }

    writeNewId(userDefinedConnId);
    LOG.debug("Will use: \"" + userDefinedConnId + "\" as AWS Connection id");

    return userDefinedConnId;
  }

  @Nullable
  @Override
  public String showNextId(@NotNull Map<String, String> props) {
    return generateNewId();
  }

  @Override
  public void addGeneratedId(@NotNull final String id, @NotNull final Map<String, String> props) {
    writeNewId(id);
  }

  public boolean isUnique(@NotNull final String connectionId) {
    return awsConnectionIdxMap.get(connectionId) == null;
  }

  @NotNull
  public Map<String, String> getAwsConnectionIdx() {
    return Collections.unmodifiableMap(awsConnectionIdxMap);
  }

  @NotNull
  private String generateNewId() {
    return makeUnique(AWS_CONNECTION_ID_PREFIX);
  }

  @NotNull
  private String makeUnique(@NotNull final String userDefinedConnId) {
    int counter = INITIAL_AWS_CONNECTION_ID;
    String newAwsConnectionId;
    do {
      newAwsConnectionId = formatId(userDefinedConnId, ++counter);
    } while (!isUnique(newAwsConnectionId));
    return newAwsConnectionId;
  }

  private void writeNewId(@NotNull String connectionId) {
    awsConnectionIdxMap.put(connectionId, connectionId);
    LOG.debug(String.format("Added AWS Connection with ID '%s'", connectionId));
  }
}