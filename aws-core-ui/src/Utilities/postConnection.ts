import {FormFields, Config, awsProviderKey, FormFieldsNames} from '../types';

import { post } from './fetchHelper';
import {encodeSecret} from "./parametersUtil";
import {AwsCredentialsType} from "../App/Components/AwsType";

type FormFieldsKey = keyof FormFields;

export function toRequestData(config: Config, data: FormFields) {
  const res = Object.keys(data).reduce<{
    [p: string]: string | null;
  }>(
    (acc, key) => {
      const value = data[key as FormFieldsKey];

      if (value !== null && value !== undefined) {
        switch (key) {
          case FormFieldsNames.AWS_SECRET_ACCESSKEY:
            const val = value.toString();
            if (/^[\x00-\x7F]*$/.test(val)) {
              acc[key] = encodeSecret(val, config.publicKey);
            } else {
              acc[key] = config.secretAccessKey;
            }
            break;
          default:
            if (typeof value === 'string') acc[key] = value;
            else if (typeof value === 'boolean') acc[key] = value.toString();
            else if (typeof value === 'object') acc[key] = value.key;
            break;
        }
      } else {
        acc[key] = null;
      }
      return acc;
    },
    {
      projectId: config.projectId,
      saveConnection: 'save',
      providerType: awsProviderKey,
    }
  );

  const credType = res[FormFieldsNames.AWS_CREDENTIALS_TYPE];

  switch (credType) {
    case AwsCredentialsType.ACCESS_KEYS.toString():
      res[FormFieldsNames.AWS_IAM_ROLE_ARN] = null;
      res[FormFieldsNames.AWS_CONNECTION_ID] = null;
      break;
    case AwsCredentialsType.IAM_ROLE.toString():
      res[FormFieldsNames.AWS_ACCESSKEY_ID] = null;
      res[FormFieldsNames.AWS_SECRET_ACCESSKEY] = null;
      break;
    default:
      res[FormFieldsNames.AWS_ACCESSKEY_ID] = null;
      res[FormFieldsNames.AWS_SECRET_ACCESSKEY] = null;
      res[FormFieldsNames.AWS_IAM_ROLE_ARN] = null;
      res[FormFieldsNames.AWS_CONNECTION_ID] = null;
      break;
  }

  return res;
}

export default async function postConnection(config: Config, data: FormFields) {
  const requestData = toRequestData(config, data);
  return await post(config.connectionsUrl, requestData);
}
