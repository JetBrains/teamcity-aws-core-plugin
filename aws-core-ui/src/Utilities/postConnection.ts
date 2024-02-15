import { FormFields, Config, awsProviderKey } from '../types';

import { post } from './fetchHelper';

type FormFieldsKey = keyof FormFields;

export function toRequestData(config: Config, data: FormFields) {
  return Object.keys(data).reduce<{
    [p: string]: string | null;
  }>(
    (acc, key) => {
      const value = data[key as FormFieldsKey];

      if (value) {
        switch (key) {
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
      // connectionId: undefined, // TODO: fix this
      providerType: awsProviderKey,
    }
  );
}

export default async function postConnection(config: Config, data: FormFields) {
  const requestData = toRequestData(config, data);
  return await post(config.connectionsUrl, requestData);
}
