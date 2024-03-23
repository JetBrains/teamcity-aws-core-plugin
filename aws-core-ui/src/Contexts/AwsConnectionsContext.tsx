import { React } from '@jetbrains/teamcity-api';
import { ReactNode, useContext, useMemo } from 'react';

import { Option } from '@jetbrains-internal/tcci-react-ui-components';
import {AwsConnection} from "../types";
import useAwsConnections from "../Hooks/useAwsConnections";

type AwsConnectionsContextType = {
  isLoading: boolean;
  connectionOptions: Option<AwsConnection>[] | undefined;
  error: string | undefined;
  reloadConnectionOptions: () => void;
};
const AwsConnectionsContext = React.createContext<AwsConnectionsContextType>({
  connectionOptions: undefined,
  error: undefined,
  isLoading: true,
  reloadConnectionOptions: () => {},
});

const { Provider } = AwsConnectionsContext;

interface OwnProps {
  children: ReactNode;
}

function AwsConnectionsContextProvider({ children }: OwnProps) {
  const value = useAwsConnections();
  const connectionOptions = useMemo(
    () => value.connectionOptions || [],
    [value.connectionOptions]
  );

  return (
    <Provider value={{ ...value, connectionOptions }}>
      {children}
    </Provider>
  );
}

const useAwsConnectionsContext = () => useContext(AwsConnectionsContext);

export { AwsConnectionsContextProvider, useAwsConnectionsContext };
