import { React } from '@jetbrains/teamcity-api';
import { useFormContext } from 'react-hook-form';

import { FormFields, FormFieldsNames } from '../types';

import { AwsCredentialsType } from './Components/AwsType';
import AccessKeysConnectionType from './Components/Type/AccessKey/AccessKeysConnectionType';
import DefaultConnectionType from './Components/Type/Default/DefaultConnectionType';
import IamRoleConnectionType from './Components/Type/IamRole/IamRoleConnectionType';

export default function SwitchTypeContent() {
  const { watch } = useFormContext<FormFields>();
  const selectedConnectionType = watch(FormFieldsNames.AWS_CREDENTIALS_TYPE);

  let key: string | undefined;
  if (typeof selectedConnectionType === 'string') {
    key = selectedConnectionType;
  } else if (typeof selectedConnectionType === 'object') {
    key = selectedConnectionType.key;
  }

  switch (key) {
    case AwsCredentialsType.ACCESS_KEYS:
      return <AccessKeysConnectionType />;
    case AwsCredentialsType.IAM_ROLE:
      return <IamRoleConnectionType />;
    case AwsCredentialsType.DEFAULT_PROVIDER:
      return <DefaultConnectionType />;
    default:
      return <div />;
  }
}
