import { useForm, UseFormReturn } from 'react-hook-form';

import {
  awsProviderKey,
  awsProviderName,
  Config,
  FormFields,
  FormFieldsNames,
} from '../types';
import { useSupportedProvidersContext } from '../Contexts/SupportedProvidersContext';
import { credentialsTypeOptions } from '../App/Components/AwsType';
import { useRegionOptions } from '../App/Components/AwsRegion';
import { PASSWORD_STUB } from '../Utilities/parametersUtil';

export default function useAwsConnectionForm(
  config: Config
): UseFormReturn<FormFields> {
  const { providers: supportedProviders } = useSupportedProvidersContext();
  const allRegions = useRegionOptions(config);
  const providerType =
    supportedProviders.find((it) => it.key === awsProviderKey) ??
    awsProviderName;

  const displayNameValue = config.displayName || 'Amazon Web Services (AWS)';
  const awsRegionValue =
    allRegions.find(
      (it) => it.key === (config.region || config.defaultRegion)
    ) ?? allRegions[0];

  const credentialsTypeValue =
    credentialsTypeOptions.find((it) => it.key === config.credentialsType) ??
    credentialsTypeOptions[0];

  const connectionIdValue = config.connectionId || undefined;

  const stsEndpointValue =
    config.stsEndpoint || `https://sts.${awsRegionValue.key}.amazonaws.com`;

  const sessionCredentialsEnabledValue = config.sessionCredentialsEnabled
    ? config.sessionCredentialsEnabled === 'true'
    : true;

  const passPlaceholder = config.secretAccessKey ? PASSWORD_STUB : undefined;

  return useForm<FormFields>({
    defaultValues: {
      [FormFieldsNames.PROVIDER_TYPE]: providerType,
      [FormFieldsNames.DISPLAY_NAME]: displayNameValue,
      [FormFieldsNames.CONNECTION_ID]: connectionIdValue,
      [FormFieldsNames.AWS_REGION]: awsRegionValue,
      [FormFieldsNames.AWS_CREDENTIALS_TYPE]: credentialsTypeValue,
      [FormFieldsNames.AWS_ACCESSKEY_ID]: config.accessKeyId || undefined,
      [FormFieldsNames.AWS_SECRET_ACCESSKEY]: passPlaceholder,
      [FormFieldsNames.AWS_SESSION_CREDENTIALS]: sessionCredentialsEnabledValue,
      [FormFieldsNames.AWS_STS_ENDPOINT]: stsEndpointValue,
      [FormFieldsNames.AWS_IAM_ROLE_SESSION_NAME]:
        config.iamRoleSessionName || 'TeamCity-session',
      [FormFieldsNames.AWS_IAM_ROLE_ARN]: config.iamRoleArn || undefined,
      [FormFieldsNames.ALLOWED_IN_SUBPROJECTS]:
        config.allowedInSubProjectsValue || false,
      [FormFieldsNames.ALLOWED_IN_BUILDS_REQUEST]:
        config.allowedInBuildsValue || false,
      [FormFieldsNames.ID]: config.id || undefined,
    },
  });
}
