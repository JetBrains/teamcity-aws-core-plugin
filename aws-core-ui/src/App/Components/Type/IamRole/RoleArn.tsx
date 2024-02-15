import { React } from '@jetbrains/teamcity-api';
import {
  FormInput,
  FormRow,
} from '@jetbrains-internal/tcci-react-ui-components';

import { useFormContext } from 'react-hook-form';
import { Size } from '@jetbrains/ring-ui/components/input/input';

import { FormFields, FormFieldsNames } from '../../../../types';

export default function RoleArn() {
  const { control } = useFormContext<FormFields>();
  return (
    <FormRow label={'Role ARN'} labelFor={FormFieldsNames.AWS_IAM_ROLE_ARN}>
      <FormInput
        control={control}
        name={FormFieldsNames.AWS_IAM_ROLE_ARN}
        details={'Pre-configured IAM role with necessary permissions'}
        size={Size.L}
      />
    </FormRow>
  );
}
