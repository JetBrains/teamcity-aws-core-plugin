import { React } from '@jetbrains/teamcity-api';
import {
  errorMessage,
  Option,
} from '@jetbrains-internal/tcci-react-ui-components';

import { post } from '../Utilities/fetchHelper';
import { AvailableConnectionsData } from '../types';

export default function useAwsConnections(data: AvailableConnectionsData) {
  const [error, setError] = React.useState<string | undefined>();
  const [connectionOptions, setConnectionOptions] = React.useState<
    Option[] | undefined
  >();
  const [isLoading, setIsLoading] = React.useState(true);

  const fetchAwsConnections = React.useCallback(async () => {
    const availableAwsConns = data.availableConnectionsControllerUrl;
    const parameters = {
      projectId: data.projectId,
      resource: data.availableConnectionsResource,
    };
    const queryComponents = new URLSearchParams(parameters).toString();
    const response = await post(`${availableAwsConns}?${queryComponents}`);

    const responseData: Array<string[]> = JSON.parse(response);

    return responseData
      .filter(
        (val) =>
          !data.awsConnectionTypesFilter ||
          data.awsConnectionTypesFilter(val[3])
      )
      .map((val) => ({ key: val[0], label: val[1] } as Option));
  }, [data]);

  const reloadConnectionOptions = React.useCallback(() => {
    fetchAwsConnections()
      .then((awsConnections) => setConnectionOptions(awsConnections))
      .catch((err: unknown) => setError(errorMessage(err)))
      .finally(() => setIsLoading(false));
  }, [fetchAwsConnections]);

  React.useEffect(() => {
    setIsLoading(true);
    reloadConnectionOptions();
  }, [reloadConnectionOptions]);

  return {
    connectionOptions,
    error,
    isLoading,
    reloadConnectionOptions,
    fetchAwsConnections,
  };
}
