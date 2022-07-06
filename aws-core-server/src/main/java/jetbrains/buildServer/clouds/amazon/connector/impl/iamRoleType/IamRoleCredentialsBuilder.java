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

package jetbrains.buildServer.clouds.amazon.connector.impl.iamRoleType;

import com.amazonaws.AmazonClientException;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.ChosenAwsConnPropertiesProcessor;
import jetbrains.buildServer.clouds.amazon.connector.impl.BaseAwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jetbrains.buildServer.clouds.amazon.connector.utils.AwsExceptionUtils.getAwsErrorMessage;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil.getResourceNameFromArn;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil.isValidSessionName;

public class IamRoleCredentialsBuilder extends BaseAwsCredentialsBuilder implements AwsCredentialsBuilder {

  private final ExecutorServices myExecutorServices;
  private final AwsConnectionsManager myAwsConnectionsManager;
  private final ProjectManager myProjectManager;

  public IamRoleCredentialsBuilder(@NotNull final AwsConnectorFactory awsConnectorFactory,
                                   @NotNull final ExecutorServices executorServices,
                                   @NotNull final AwsConnectionsManager awsConnectionsManager,
                                   @NotNull final ProjectManager projectManager) {
    awsConnectorFactory.registerAwsCredentialsBuilder(this);
    myExecutorServices = executorServices;
    myAwsConnectionsManager = awsConnectionsManager;
    myProjectManager = projectManager;
  }

  @Override
  @NotNull
  public AwsCredentialsHolder constructConcreteCredentialsProvider(@NotNull final Map<String, String> cloudConnectorProperties) throws AwsConnectorException {

    throwExceptionIfPropertiesInvalid(validateProperties(cloudConnectorProperties));

    String projectId = cloudConnectorProperties.get(IAM_ROLE_PROJECT_ID_PARAM);
    SProject project = myProjectManager.findProjectByExternalId(projectId);

    if (project == null) {
      throw new AwsConnectorException("Failed to find project with id: " + projectId);
    }

    try {
      AwsConnectionBean principalAwsConnection = myAwsConnectionsManager.getLinkedAwsConnection(cloudConnectorProperties, project);

      return new IamRoleSessionCredentialsHolder(
        principalAwsConnection.getAwsCredentialsHolder(),
        cloudConnectorProperties,
        myExecutorServices
      );
    } catch (AmazonClientException ace) {
      throw new AwsConnectorException("Failed to get the principal AWS connection to assume IAM Role: " + getAwsErrorMessage(ace));
    }
  }

  @Override
  @NotNull
  public List<InvalidProperty> validateProperties(@NotNull final Map<String, String> properties) {

    ChosenAwsConnPropertiesProcessor chosenAwsConnPropertiesProcessor = new ChosenAwsConnPropertiesProcessor();
    List<InvalidProperty> invalidProperties =
      new ArrayList<>(chosenAwsConnPropertiesProcessor.process(properties));

    if (StringUtil.isEmpty(properties.get(IAM_ROLE_ARN_PARAM))) {
      invalidProperties.add(new InvalidProperty(IAM_ROLE_ARN_PARAM, "Please provide the ARN of IAM Role to assume."));
    }

    if (!isValidSessionName(properties.get(IAM_ROLE_SESSION_NAME_PARAM))) {
      invalidProperties.add(
        new InvalidProperty(IAM_ROLE_SESSION_NAME_PARAM, "The Session Name is not valid, must satisfy regular expression pattern: " + VALID_ROLE_SESSION_NAME_REGEX));
    }

    return invalidProperties;
  }

  @Override
  @NotNull
  public String getCredentialsType() {
    return AwsCloudConnectorConstants.IAM_ROLE_CREDENTIALS_TYPE;
  }

  @Override
  @NotNull
  public String getPropertiesDescription(@NotNull final Map<String, String> properties) {
    return
      "Assume " +
        getResourceNameFromArn(properties.get(IAM_ROLE_ARN_PARAM)) +
        " role to gain temporary credentials with specified privileges";
  }
}
