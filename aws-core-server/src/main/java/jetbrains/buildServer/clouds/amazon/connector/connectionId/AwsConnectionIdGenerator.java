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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.serverSide.oauth.identifiers.OAuthConnectionsIdGenerator;
import jetbrains.buildServer.util.CachingTypedIdGenerator;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.USER_DEFINED_ID_PARAM;

public class AwsConnectionIdGenerator extends BuildServerAdapter implements CachingTypedIdGenerator {
  public final static String ID_GENERATOR_TYPE = AwsConnectionProvider.TYPE;
  public final static String AWS_CONNECTION_ID_PREFIX = "awsConnection";

  private final static Logger LOG = Logger.getInstance(AwsConnectionIdGenerator.class.getName());

  private final ConcurrentHashMap<String, String> awsConnectionIdxMap = new ConcurrentHashMap<>();

  private final ProjectManager myProjectManager;
  private final ExecutorServices myExecutorServices;
  private AwsConnectionIdSynchroniser myAwsConnectionIdSynchroniser;

  public AwsConnectionIdGenerator(@NotNull final OAuthConnectionsIdGenerator OAuthConnectionsIdGenerator,
                                  @NotNull final EventDispatcher<BuildServerListener> eventDispatcher,
                                  @NotNull final ProjectManager projectManager,
                                  @NotNull final ExecutorServices executorServices) {
    OAuthConnectionsIdGenerator.registerProviderTypeGenerator(ID_GENERATOR_TYPE, this);
    eventDispatcher.addListener(this);

    myProjectManager = projectManager;
    myExecutorServices = executorServices;
  }

  @Nullable
  @Override
  public String newId(@NotNull Map<String, String> props) {
    String userDefinedConnId = props.get(USER_DEFINED_ID_PARAM);

    boolean needToGenerateId = false;
    if (userDefinedConnId == null) {
      needToGenerateId = true;
      LOG.debug("User did not define the connection id, will generate it using incremental ID");
    } else if (!isUnique(userDefinedConnId)) {
      needToGenerateId = true;
      LOG.warn("User-defined connection id is not unique, will generate it using incremental ID");
    }
    if (needToGenerateId) {
      userDefinedConnId = generateNewId();
      props.put(USER_DEFINED_ID_PARAM, userDefinedConnId);
    }

    writeNewId(userDefinedConnId);
    LOG.debug("Will use: \"" + userDefinedConnId + "\" as AWS Connection id");

    return userDefinedConnId;
  }

  @Override
  public void addGeneratedId(@NotNull final String id, @NotNull final Map<String, String> props) {
    if (!isUnique(id)) {
      LOG.warn("Generated AWS Connection ID is not unique, check that your Project does not have another AWS Connection with ID: " + id);
    }
    writeNewId(id);
  }

  public boolean isUnique(@NotNull final String connectionId) {
    return awsConnectionIdxMap.get(connectionId) == null;
  }

  @NotNull
  public Map<String, String> getAwsConnectionIdx() {
    return Collections.unmodifiableMap(awsConnectionIdxMap);
  }

  public boolean currentIdentifierInitialised() {
    return myAwsConnectionIdSynchroniser.currentIdentifierInitialised();
  }

  @Override
  public void serverStartup() {
    myAwsConnectionIdSynchroniser = new AwsConnectionIdSynchroniser(
      myProjectManager.getRootProject()
    );
    myAwsConnectionIdSynchroniser.setInitialIdentifier();

    myExecutorServices
      .getLowPriorityExecutorService()
      .submit(
        new AwsConnectionIdSyncTask(
          myExecutorServices,
          myAwsConnectionIdSynchroniser
        )
      );
  }

  @Override
  public void serverShutdown() {
    myAwsConnectionIdSynchroniser.syncIdentifier();
  }

  @NotNull
  private String generateNewId() {
    int newIdNumber;

    if (!myAwsConnectionIdSynchroniser.currentIdentifierInitialised()) {
      Random r = new Random();
      newIdNumber = 100000 + r.nextInt(100000);
    } else {
      newIdNumber = myAwsConnectionIdSynchroniser.incrementAndGetCurrentIdentifier();
      while (!isUnique(String.valueOf(newIdNumber))) {
        newIdNumber = myAwsConnectionIdSynchroniser.incrementAndGetCurrentIdentifier();
      }
    }

    return String.format("%s-%s", AWS_CONNECTION_ID_PREFIX, newIdNumber);
  }

  private void writeNewId(@NotNull String connectionId) {
    awsConnectionIdxMap.put(connectionId, connectionId);
    LOG.debug(String.format("Added AWS Connection with ID '%s'", connectionId));
  }
}