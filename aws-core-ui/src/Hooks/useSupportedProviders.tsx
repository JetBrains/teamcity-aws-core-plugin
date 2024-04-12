import { React } from '@jetbrains/teamcity-api';
import {
  errorMessage,
  Option,
} from '@jetbrains-internal/tcci-react-ui-components';

import { Config } from '../types';
import { get } from '../Utilities/fetchHelper';

export default function useSupportedProviders({ config }: { config: Config }) {
  const [loading, setLoading] = React.useState<boolean>(false);
  const [error, setError] = React.useState<string | undefined>(undefined);
  const [data, setData] = React.useState<Option[]>([]);

  const loadProviders = React.useCallback(async () => {
    setLoading(true);
    setError(undefined);
    try {
      const params = new URLSearchParams();
      params.append('projectId', config.projectId);
      const response = await get(
        `${config.supportedProvidersUrl}?${params.toString()}`
      );
      const parsedResponse = JSON.parse(response);
      return Object.keys(parsedResponse).map((key) => ({
        key,
        label: parsedResponse[key],
      }));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }

    // return previous value if error occurred
    return [];
  }, [config.projectId, config.supportedProvidersUrl]);

  React.useEffect(() => {
    loadProviders().then((response) => setData(response));
  }, [loadProviders]);

  return { loading, error, data };
}
