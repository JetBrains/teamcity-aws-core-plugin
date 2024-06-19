import { React } from '@jetbrains/teamcity-api';

import { useState } from 'react';

import { Config } from '../types';

type ApplicationContextType = {
  config: Config;
  isCreateMode: boolean;
  isEditMode: boolean;
  setConfig: (newConfig: Config) => void;
};

const ApplicationContext = React.createContext<ApplicationContextType>({
  config: {
    id: '',
    connectionId: '',
    disableTypeSelection: false,
    projectId: '',
    supportedProvidersUrl: '',
    availableAwsConnectionsControllerResource: '',
    availableAwsConnectionsControllerUrl: '',
    connectionsUrl: '',
    displayName: '',
    region: '',
    defaultRegion: '',
    credentialsType: '',
    accessKeyId: '',
    secretAccessKey: '',
    sessionCredentialsEnabled: '',
    stsEndpoint: '',
    iamRoleArn: '',
    iamRoleSessionName: '',
    buildStepsFeatureEnabled: false,
    subProjectsFeatureEnabled: false,
    allowedInSubProjectsValue: false,
    allowedInBuildsValue: false,
    publicKey: '',
    featureId: '',
    testConnectionUrl: '',
    awsConnectionId: '',
    allRegions: {
      allRegionKeys: '',
      allRegionValues: '',
    },
    isDefaultCredProviderEnabled: false,
    rotateKeyControllerUrl: '',
  },
  isCreateMode: false,
  isEditMode: false,
  setConfig: (_newConfig) => {},
});

const { Provider } = ApplicationContext;

function ApplicationContextProvider({
  config,
  children,
}: {
  config: Config;
  children: React.ReactNode;
}) {
  const [newConfig, setNewConfig] = useState(config);
  return (
    <Provider
      value={{
        config: newConfig,
        isCreateMode: !config.connectionId,
        isEditMode: !!config.connectionId,
        setConfig: (newConfig) => setNewConfig(newConfig),
      }}
    >
      {children}
    </Provider>
  );
}

const useApplicationContext = () => React.useContext(ApplicationContext);

export { ApplicationContextProvider, useApplicationContext };
