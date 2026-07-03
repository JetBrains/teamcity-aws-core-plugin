import { React } from '@jetbrains/teamcity-api';
import { Option } from '@jetbrains-internal/tcci-react-ui-components';

import useSupportedProviders from '../Hooks/useSupportedProviders';

import { useApplicationContext } from './ApplicationContext';

type SupportedProvidersType = {
  providers: Option[];
  loading: boolean;
  error: string | undefined;
};

const SupportedProvidersContext = React.createContext<SupportedProvidersType>({
  providers: [],
  loading: false,
  error: undefined,
});

const { Provider } = SupportedProvidersContext;

function SupportedProvidersContextProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const { config } = useApplicationContext();
  const { data: providers, error, loading } = useSupportedProviders({ config });

  return <Provider value={{ providers, loading, error }}>{children}</Provider>;
}

const useSupportedProvidersContext = () =>
  React.useContext(SupportedProvidersContext);

export { SupportedProvidersContextProvider, useSupportedProvidersContext };
