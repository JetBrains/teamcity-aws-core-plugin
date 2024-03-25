import { useCallback, useEffect, useState } from 'react';

import {
  errorMessage,
  Option,
} from '@jetbrains-internal/tcci-react-ui-components';

import { post } from '../Utilities/fetchHelper';
import {useApplicationContext} from "../Contexts/ApplicationContext";

export default function useAwsConnections() {
  const ctx = useApplicationContext();
  const [error, setError] = useState<string | undefined>();
  const [connectionOptions, setConnectionOptions] = useState<
    Option[] | undefined >();
  const [isLoading, setIsLoading] = useState(true);

  const fetchAwsConnections = useCallback(async () => {
    const availableAwsConns = ctx.config.availableAwsConnectionsControllerUrl;
    const parameters = {
      projectId: ctx.config.projectId,
      resource: ctx.config.availableAwsConnectionsControllerResource,
    };
    const queryComponents = new URLSearchParams(parameters).toString();
    const response = await post(
      `${availableAwsConns}?${queryComponents}`
    );

    const responseData: Array<string[]> = JSON.parse(response);

    return responseData.reduce((processedValue, dataValue) => {
      processedValue.push({
        key: dataValue[0],
        label: dataValue[1]
        } as Option);
      return processedValue;
    }, [] as Option[]);
  }, [ctx]);

  const reloadConnectionOptions = useCallback(() => {
    fetchAwsConnections()
      .then((awsConnections) => setConnectionOptions(awsConnections))
      .catch((err: unknown) => setError(errorMessage(err)))
      .finally(() => setIsLoading(false));
  }, [fetchAwsConnections]);

  useEffect(() => {
    setIsLoading(true);
    reloadConnectionOptions();
  }, [reloadConnectionOptions]);

  return { connectionOptions, error, isLoading, reloadConnectionOptions, fetchAwsConnections };
}
