import { React } from '@jetbrains/teamcity-api';
import { Option } from '@jetbrains-internal/tcci-react-ui-components';

export const awsProviderKey = 'AWS';
export const awsProviderName = 'Amazon Web Services (AWS)';

export interface Config {
  projectId: string;
  connectionId: string;
  supportedProvidersUrl: string;
  connectionsUrl: string;
  availableAwsConnectionsControllerResource: string;
  availableAwsConnectionsControllerUrl: string;
  displayName: string;
  region: string;
  defaultRegion: string;
  credentialsType: string;
  accessKeyId: string;
  secretAccessKey: string;
  sessionCredentialsEnabled: string;
  stsEndpoint: string;
  iamRoleArn: string;
  iamRoleSessionName: string;
  buildStepsFeatureEnabled: boolean;
  subProjectsFeatureEnabled: boolean;
  allowedInSubProjectsValue: boolean;
  allowedInBuildsValue: boolean;
  publicKey: string;
  featureId: string;
  testConnectionUrl: string;
  awsConnectionId: string;
  allRegions: {
    allRegionKeys: string;
    allRegionValues: string;
  };
  isDefaultCredProviderEnabled: boolean;
  // customization section of the config file
  onClose?: () => void;
  disableTypeSelection?: boolean;
  id: string;
  afterSubmit?: (
    data: FormFields,
    isError: boolean,
    response?: Document,
    event?: React.BaseSyntheticEvent
  ) => void;
}

export enum FormFieldsNames {
  PROVIDER_TYPE = '__providerType',
  DISPLAY_NAME = 'prop:displayName',
  FEATURE_ID = 'prop:featureId',
  ID = 'prop:id',
  CONNECTION_ID = 'connectionId',
  AWS_REGION = 'prop:awsRegionName',
  AWS_CREDENTIALS_TYPE = 'prop:awsCredentialsType',
  AWS_ACCESSKEY_ID = 'prop:awsAccessKeyId',
  AWS_SECRET_ACCESSKEY = 'prop:encrypted:secure:awsSecretAccessKey',
  AWS_SESSION_CREDENTIALS = 'prop:awsSessionCredentials',
  AWS_STS_ENDPOINT = 'prop:awsStsEndpoint',
  AWS_IAM_ROLE_ARN = 'prop:awsIamRoleArn',
  AWS_CONNECTION_ID = 'prop:awsConnectionId',
  AWS_IAM_ROLE_SESSION_NAME = 'prop:awsIamRoleSessionName',
  ALLOWED_IN_BUILDS_REQUEST = 'prop:forBuilds',
  ALLOWED_IN_SUBPROJECTS = 'prop:awsAllowedInSubProjects',
}

const formFieldsKeys = [
  FormFieldsNames.PROVIDER_TYPE,
  FormFieldsNames.DISPLAY_NAME,
  FormFieldsNames.FEATURE_ID,
  FormFieldsNames.ID,
  FormFieldsNames.CONNECTION_ID,
  FormFieldsNames.AWS_REGION,
  FormFieldsNames.AWS_CREDENTIALS_TYPE,
  FormFieldsNames.AWS_ACCESSKEY_ID,
  FormFieldsNames.AWS_SECRET_ACCESSKEY,
  FormFieldsNames.AWS_SESSION_CREDENTIALS,
  FormFieldsNames.AWS_STS_ENDPOINT,
  FormFieldsNames.AWS_IAM_ROLE_ARN,
  FormFieldsNames.AWS_CONNECTION_ID,
  FormFieldsNames.AWS_IAM_ROLE_SESSION_NAME,
  FormFieldsNames.ALLOWED_IN_BUILDS_REQUEST,
  FormFieldsNames.ALLOWED_IN_SUBPROJECTS,
];

interface FormFieldsBase {
  [FormFieldsNames.PROVIDER_TYPE]: Option | string;
  [FormFieldsNames.DISPLAY_NAME]: string;
  [FormFieldsNames.FEATURE_ID]: string;
  [FormFieldsNames.ID]: string;
  [FormFieldsNames.CONNECTION_ID]: string;
  [FormFieldsNames.AWS_REGION]: Option | string;
  [FormFieldsNames.AWS_CREDENTIALS_TYPE]: Option | string;
  [FormFieldsNames.AWS_ACCESSKEY_ID]: string;
  [FormFieldsNames.AWS_SECRET_ACCESSKEY]: string;
  [FormFieldsNames.AWS_SESSION_CREDENTIALS]: boolean;
  [FormFieldsNames.AWS_STS_ENDPOINT]: string;
  [FormFieldsNames.AWS_IAM_ROLE_ARN]: string;
  [FormFieldsNames.AWS_CONNECTION_ID]: Option | string;
  [FormFieldsNames.AWS_IAM_ROLE_SESSION_NAME]: string;
  [FormFieldsNames.ALLOWED_IN_BUILDS_REQUEST]: boolean;
  [FormFieldsNames.ALLOWED_IN_SUBPROJECTS]: boolean;
}

export type FormFields = Partial<FormFieldsBase>;

export function errorKeyToFieldNameConvertor(key: string): string | null {
  if (key === 'unexpected') {
    return FormFieldsNames.CONNECTION_ID;
  }
  return formFieldsKeys.find((it) => it.endsWith(key)) ?? null;
}

const helpUrlPrefix = (window.BS?.helpUrlPrefix ?? '').replace(/\?$/, '');
export const resolveHelpURL = (page: string): string =>
  `${helpUrlPrefix}${page}`;

export type AwsConnection = string & {
  displayName: string;
  id: string;
  usingSessionCreds: boolean;
};
