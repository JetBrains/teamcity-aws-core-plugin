/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

import { Option } from '@jetbrains-internal/tcci-react-ui-components';

import { Config, AwsConnectionData, FormFieldsNames } from '../types';

export function encodeSecret(value: string, publicKey: string): string {
  return window.BS.Encrypt.encryptData(value, publicKey);
}

export const PASSWORD_STUB = '\u2022'.repeat(40);

export function toConfig(
  data: AwsConnectionData,
  onClose: () => void,
  onCreated?: (connectionId: string) => void
): Config {
  return {
    id: '',
    connectionId: '',
    disableTypeSelection: true,
    projectId: data.projectId,
    supportedProvidersUrl: '',
    availableAwsConnectionsControllerResource:
      data.awsAvailableConnectionsResource,
    availableAwsConnectionsControllerUrl:
      data.awsAvailableConnectionsControllerUrl,
    connectionsUrl: data.awsConnectionsUrl,
    displayName: '',
    region: data.region,
    defaultRegion: '',
    credentialsType: data.credentialsType,
    accessKeyId: data.key,
    secretAccessKey: encodeSecret(data.secret, data.publicKey),
    sessionCredentialsEnabled: '',
    stsEndpoint: '',
    iamRoleArn: '',
    iamRoleSessionName: '',
    buildStepsFeatureEnabled: false,
    subProjectsFeatureEnabled: false,
    allowedInSubProjectsValue: false,
    allowedInBuildsValue: false,
    publicKey: data.publicKey,
    featureId: '',
    testConnectionUrl: data.testConnectionsUrl,
    awsConnectionId: '',
    allRegions: {
      allRegionKeys: data.allRegionKeys,
      allRegionValues: data.allRegionValues,
    },
    isDefaultCredProviderEnabled: data.defaultProviderChain,
    onClose: () => {
      onClose();
    },
    afterSubmit: (formData, isError, _response, _event) => {
      if (isError) {
        return;
      }

      data.onSuccess({
        key: formData[FormFieldsNames.ID] as string,
        label: formData[FormFieldsNames.DISPLAY_NAME],
      } as Option);

      if (onCreated) {
        onCreated(formData[FormFieldsNames.ID] as string);
      }

      onClose();
    },
    awsConnectionTypesFilter: data.awsConnectionTypesFilter,
  } as Config;
}
