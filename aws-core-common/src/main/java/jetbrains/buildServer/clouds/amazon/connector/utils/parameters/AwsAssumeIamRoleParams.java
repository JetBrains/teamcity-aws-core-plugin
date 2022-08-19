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

package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.AVAIL_AWS_CONNS_JSP_FILE_NAME;

public final class AwsAssumeIamRoleParams {

  public static final String IAM_ROLE_LABEL = "IAM Role";

  public static final String IAM_ROLE_ARN_PARAM = "awsIamRoleArn";
  public static final String PRINCIPAL_AWS_CONN_PROJECT_ID_PARAM = "principalAwsConnectionProjectId";
  public static final String IAM_ROLE_ARN_LABEL = "Role ARN:";
  public static final String IAM_ROLE_SESSION_NAME_PARAM = "awsIamRoleSessionName";
  public static final String IAM_ROLE_SESSION_NAME_LABEL = "Session name tag:";
  public static final String IAM_ROLE_SESSION_NAME_DEFAULT = "TeamCity-session";

  public static final String STS_ENDPOINT_FIELD_ID_IAM_ROLE = "stsEndpointFieldIamRole";

  public static final String VALID_ROLE_SESSION_NAME_REGEX = "[\\w+=,.@-]*";

  public static final String AVAIL_AWS_CONNS_URL = "../../availableAwsConnections/" + AVAIL_AWS_CONNS_JSP_FILE_NAME;
}
