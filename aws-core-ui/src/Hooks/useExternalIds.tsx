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

import { React } from '@jetbrains/teamcity-api';

import { errorMessage } from '@jetbrains-internal/tcci-react-ui-components';

import { Config } from '../types';
import { post } from '../Utilities/fetchHelper';

export default function useExternalIds(config: Config) {
  const [error, setError] = React.useState<string | undefined>();
  const [externalId, setExternalId] = React.useState<string | undefined>();

  const [isLoading, setIsLoading] = React.useState(true);
  const fetchExternalId = React.useCallback(async () => {
    const postUrl = config.externalIdsControllerUrl;
    const parameters = {
      projectId: config.projectId,
      [config.externalIdsConnectionParam]: config.connectionId,
    };
    const queryComponents = new URLSearchParams(parameters).toString();
    const response = await post(`${postUrl}?${queryComponents}`);

    return JSON.parse(response);
  }, [
    config.externalIdsControllerUrl,
    config.projectId,
    config.externalIdsConnectionParam,
    config.connectionId,
  ]);

  React.useEffect(() => {
    setIsLoading(true);
    fetchExternalId()
      .then((newId) => setExternalId(newId))
      .catch((err: unknown) => setError(errorMessage(err)))
      .finally(() => setIsLoading(false));
  }, [fetchExternalId]);

  return {
    externalId,
    error,
    isLoading,
  };
}
