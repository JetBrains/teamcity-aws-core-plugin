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

import jetbrains.buildServer.clouds.CloudException;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.impl.IdGeneratorRegistry;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorFactory;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.util.CachingTypedIdGenerator;
import jetbrains.buildServer.util.IdentifiersGenerator;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.USER_DEFINED_ID_PARAM;

public class AwsConnectionIdGenerator implements CachingTypedIdGenerator {

  public final static String AWS_CONNECTIONS_IDX_FOLDER = "awsConnectionsIdx";
  public final static String AWS_CONNECTIONS_IDX_FILE = "awsConnections.idx";
  private final ReadWriteLock myRWLock = new ReentrantReadWriteLock();
  private final File myAwsConnectionsIdxFile;
  private final IdentifiersGenerator myDefaultIdGenerator;

  public AwsConnectionIdGenerator(@NotNull final ServerPaths serverPaths,
                                  @NotNull ProjectFeatureDescriptorFactory featureDescriptorFactory,
                                  @NotNull IdGeneratorRegistry idGeneratorRegistry) {

    myDefaultIdGenerator = idGeneratorRegistry.acquireIdGenerator("PROJECT_EXT_");

    File awsConnectionsIdxFolder = new File(serverPaths.getPluginDataDirectory(), AWS_CONNECTIONS_IDX_FOLDER);
    if (!awsConnectionsIdxFolder.exists()) {
      awsConnectionsIdxFolder.mkdirs();
    }
    if (!awsConnectionsIdxFolder.exists()) {
      throw new CloudException("Unable to create the folder for aws connection idx at " + awsConnectionsIdxFolder.getAbsolutePath());
    }

    myAwsConnectionsIdxFile = new File(awsConnectionsIdxFolder, AWS_CONNECTIONS_IDX_FILE);
    try {
      if (!myAwsConnectionsIdxFile.createNewFile()) {
        boolean fileIsDeleted = myAwsConnectionsIdxFile.delete();
        if (!fileIsDeleted) {
          throw new CloudException("Unable to clean the file for aws connection idx at " + myAwsConnectionsIdxFile.getAbsolutePath());
        }
        myAwsConnectionsIdxFile.createNewFile();
      }
    } catch (IOException e) {
      throw new CloudException("Unable to create the file for aws connection idx at " + myAwsConnectionsIdxFile.getAbsolutePath());
    }

    featureDescriptorFactory.registerGenerator(OAuthConstants.FEATURE_TYPE, this);
  }

  @Nullable
  @Override
  public String newId(Map<String, String> props) {
    if (otherConnectionType(props)) {
      return myDefaultIdGenerator.newId();
    }

    String userDefinedConnId = props.get(USER_DEFINED_ID_PARAM);
    if (userDefinedConnId == null) {
      Loggers.CLOUD.info("User did not define the connection id, will use UUID");
      userDefinedConnId = UUID.randomUUID().toString();
      props.put(USER_DEFINED_ID_PARAM, userDefinedConnId);
    } else if (!isUnique(userDefinedConnId)) {
      Loggers.CLOUD.warn("User-defined connection id is not unique, will use UUID");
      userDefinedConnId = UUID.randomUUID().toString();
      props.put(USER_DEFINED_ID_PARAM, userDefinedConnId);
    }

    writeNewId(userDefinedConnId);
    Loggers.CLOUD.debug("Will use: \"" + userDefinedConnId + "\" as AWS Connection id");

    return userDefinedConnId;
  }

  @Override
  public void addGeneratedId(@NotNull final String id) {
    if (!isUnique(id)) {
      Loggers.CLOUD.warn("Generated AWS Connection ID is not unique, please, change it and check that the file " + myAwsConnectionsIdxFile.getAbsolutePath() + " is available");
      return;
    }
    writeNewId(id);
  }

  public boolean isUnique(@NotNull final String connectionId) {
    String newIdSha1 = DigestUtils.sha1Hex(connectionId);

    myRWLock.readLock().lock();
    try (BufferedReader reader = new BufferedReader(new FileReader(myAwsConnectionsIdxFile))) {
      String line = reader.readLine();
      while (line != null) {
        if (line.equals(newIdSha1)) {
          return false;
        }
        line = reader.readLine();
      }
    } catch (IOException e) {
      Loggers.CLOUD.warnAndDebugDetails(String.format("Unable to read AWS Connection idx file '%s'", myAwsConnectionsIdxFile.getAbsolutePath()), e);
      return false;
    } finally {
      myRWLock.readLock().unlock();
    }
    return true;
  }

  private boolean otherConnectionType(Map<String, String> props) {
    return !props.containsKey(CREDENTIALS_TYPE_PARAM);
  }

  private void writeNewId(@NotNull String connectionId) {
    myRWLock.writeLock().lock();
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(myAwsConnectionsIdxFile, true))) {
      bw.write(DigestUtils.sha1Hex(connectionId));
      bw.newLine();
    } catch (IOException e) {
      Loggers.CLOUD.warnAndDebugDetails(String.format("Unable to write AWS Connection ID to the file '%s'", myAwsConnectionsIdxFile.getAbsolutePath()), e);
    } finally {
      myRWLock.writeLock().unlock();
    }
  }
}