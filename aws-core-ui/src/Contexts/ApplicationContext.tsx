import { React } from '@jetbrains/teamcity-api';

import { Config } from '../types';

type ApplicationContextType = {
  config: Config;
  isCreateMode: boolean;
  isEditMode: boolean;
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
  },
  isCreateMode: false,
  isEditMode: false,
});

const { Provider } = ApplicationContext;

function ApplicationContextProvider({
  config,
  children,
}: {
  config: Config;
  children: React.ReactNode;
}) {
  return (
    <Provider
      value={{
        config,
        isCreateMode: !config.connectionId,
        isEditMode: !!config.connectionId,
      }}
    >
      {children}
    </Provider>
  );
}

const useApplicationContext = () => React.useContext(ApplicationContext);

export { ApplicationContextProvider, useApplicationContext };
