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

package jetbrains.buildServer.clouds.amazon.connector.impl;

import java.util.Map;
import java.util.Random;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.serverSide.oauth.identifiers.OAuthConnectionsIdGenerator;
import jetbrains.buildServer.util.CachingTypedIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.USER_DEFINED_ID_PARAM;

public class AwsConnectionIdGenerator implements CachingTypedIdGenerator {

  public final static String AWS_CONNECTIONS_IDX_STORAGE = "aws.connections.idx.storage";
  public final static String AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM = "awsConnectionsCurrentId";
  public final static int FIRST_INCREMENTAL_ID = 0;
  public final static String ID_GENERATOR_TYPE = AwsConnectionProvider.TYPE;
  public final static String AWS_CONNECTION_ID_PREFIX = "awsConnection";

  private final ProjectManager myProjectManager;

  private boolean awsConnectionsIdxStorageWasAccessed;
  public AwsConnectionIdGenerator(@NotNull OAuthConnectionsIdGenerator OAuthConnectionsIdGenerator,
                                  @NotNull final ProjectManager projectManager) {
    myProjectManager = projectManager;
    OAuthConnectionsIdGenerator.registerProviderTypeGenerator(ID_GENERATOR_TYPE, this);

    awsConnectionsIdxStorageWasAccessed = false;
  }

  @Nullable
  @Override
  public String newId(Map<String, String> props) {

    String userDefinedConnId = props.get(USER_DEFINED_ID_PARAM);
    boolean needToGenerateId = false;
    if (userDefinedConnId == null) {
      needToGenerateId = true;
      Loggers.CLOUD.debug("User did not define the connection id, will generate it using incremental ID");
    } else if (!isUnique(userDefinedConnId)) {
      needToGenerateId = true;
      Loggers.CLOUD.warn("User-defined connection id is not unique, will use UUID");
    }
    if (needToGenerateId) {
      userDefinedConnId = generateNewId();
      props.put(USER_DEFINED_ID_PARAM, userDefinedConnId);
    }

    writeNewId(userDefinedConnId);
    Loggers.CLOUD.debug("Will use: \"" + userDefinedConnId + "\" as AWS Connection id");

    return userDefinedConnId;
  }

  @Override
  public void addGeneratedId(@NotNull final String id, @NotNull final Map<String,String> props) {
    if (!isUnique(id)) {
      Loggers.CLOUD.warn("Generated AWS Connection ID is not unique, check that your Project does not have another AWS Connection with ID: " + id);
    }
    writeNewId(id);
  }

  public boolean isUnique(@NotNull final String connectionId) {
    final CustomDataStorage storage = getDataStorage();
    final Map<String, String> values = storage.getValues();
    if (values == null) return true;

    return values.containsKey(connectionId);
  }

  @NotNull
  private synchronized String generateNewId() {
    final CustomDataStorage storage = getDataStorage();
    final Map<String, String> values = storage.getValues();

    int newIdNumber;
    if (values == null || values.get(AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM) == null) {
      storage.putValue(AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM, String.valueOf(FIRST_INCREMENTAL_ID));
      newIdNumber = FIRST_INCREMENTAL_ID;

    } else {
      try {
        newIdNumber = Integer.parseInt(values.get(AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM));
        newIdNumber++;
        storage.putValue(AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM, String.valueOf(newIdNumber));

      } catch (NumberFormatException e) {
        Loggers.CLOUD.warnAndDebugDetails("Wrong number in the incremental ID parameter of the CustomDataStorage in the Root Project", e);
        Random r = new Random();
        newIdNumber = 100000 + r.nextInt(100000);
        storage.putValue(AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM, String.valueOf(newIdNumber));
      }
    }
    storage.flush();

    return String.format("%s-%s", AWS_CONNECTION_ID_PREFIX, newIdNumber);
  }

  private void writeNewId(@NotNull String connectionId) {
    final CustomDataStorage storage = getDataStorage();
    storage.refresh();
    storage.putValue(connectionId, connectionId);
    storage.flush();
    Loggers.CLOUD.debug(String.format("Added AWS Connection with ID '%s'", connectionId));
  }

  @NotNull
  private CustomDataStorage getDataStorage() {
    if (awsConnectionsIdxStorageWasAccessed) {
      return myProjectManager.getRootProject().getCustomDataStorage(AWS_CONNECTIONS_IDX_STORAGE);
    } else {
      CustomDataStorage storage = myProjectManager.getRootProject().getCustomDataStorage(AWS_CONNECTIONS_IDX_STORAGE);
      final Map<String, String> values = storage.getValues();

      String currentConnectionIdNumber = null;
      if (values != null){
        currentConnectionIdNumber = values.get(AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM);
      }
      if (currentConnectionIdNumber == null) {
        currentConnectionIdNumber = String.valueOf(FIRST_INCREMENTAL_ID);
      }
      storage.clear();
      storage.putValue(AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM, currentConnectionIdNumber);
      storage.flush();
      awsConnectionsIdxStorageWasAccessed = true;
      return storage;
    }
  }
}