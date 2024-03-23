import { React } from '@jetbrains/teamcity-api';

import { Config } from '../types';

type ApplicationContextType = {
  config: Config;
  isCreateMode: boolean;
  isEditMode: boolean;
};

//here be dragons:
//if connection id is empty when the data is received aka config -> it means it's a new connection
//in this case you'd put new connection id in prop:id field
//otherwise, it's non-editable and no shenanigans are required
//awsConnectionId has nothing to do with a new or old connection
//it's a principal connection for an iam type connection

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
      allRegionValues: ''
    },
    isDefaultCredProviderEnabled: false
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
