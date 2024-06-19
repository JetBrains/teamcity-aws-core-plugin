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

import { Config } from '../types';

import { post } from './fetchHelper';
import { getConfigForConnection } from './responseParserUtils';

export async function requestKeyRotation(
  config: Config
): Promise<RotateKeysResult> {
  const params = {
    connectionId: config.connectionId,
    projectId: config.projectId,
  };

  const response = await post(config.rotateKeyControllerUrl, params);

  const result = JSON.parse(response);

  if (result.errors && result.errors.length !== 0) {
    return { key: '', secret: '', errorMessage: result.errors[0].message };
  }

  const updatedConnection = await getConfigForConnection(
    config.projectId,
    config.connectionId
  );

  if (!updatedConnection) {
    return {
      key: '',
      secret: '',
      errorMessage:
        'Failed to receive a server response describing the updated connection',
    };
  }

  return {
    key: updatedConnection.accessKeyId,
    secret: updatedConnection.secretAccessKey,
  };
}

interface RotateKeysResult {
  key: string;
  secret: string;
  errorMessage?: string;
}
