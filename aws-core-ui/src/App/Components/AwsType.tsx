import { React } from '@jetbrains/teamcity-api';
import {
  FormRow,
  FormSelect,
  Option,
} from '@jetbrains-internal/tcci-react-ui-components';

import { useFormContext } from 'react-hook-form';
import { Size } from '@jetbrains/ring-ui/components/input/input';

import { FormFields, FormFieldsNames } from '../../types';

import appStyles from '../styles.css';

export enum AwsCredentialsType {
  ACCESS_KEYS = 'awsAccessKeys',
  IAM_ROLE = 'awsAssumeIamRole',
  DEFAULT_PROVIDER = 'defaultProvider',
}

export const credentialsTypeOptions: Option[] = [
  { key: AwsCredentialsType.ACCESS_KEYS, label: 'Access keys' },
  { key: AwsCredentialsType.IAM_ROLE, label: 'IAM role' },
  {
    key: AwsCredentialsType.DEFAULT_PROVIDER,
    label: 'Default Credential Provider Chain',
  },
];

export default function AwsType() {
  const { control } = useFormContext<FormFields>();
  return (
    <FormRow label={'Type'} labelFor={FormFieldsNames.AWS_CREDENTIALS_TYPE}>
      <FormSelect
        name={FormFieldsNames.AWS_CREDENTIALS_TYPE}
        control={control}
        size={Size.L}
        popupClassName={appStyles.dropDownPopup}
        data={credentialsTypeOptions}
      />
    </FormRow>
  );
}
