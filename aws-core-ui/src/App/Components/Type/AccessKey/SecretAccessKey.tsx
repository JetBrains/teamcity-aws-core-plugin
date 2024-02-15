import { React } from '@jetbrains/teamcity-api';
import { useFormContext } from 'react-hook-form';
import {
  FormInput,
  FormRow,
} from '@jetbrains-internal/tcci-react-ui-components';
import { Size } from '@jetbrains/ring-ui/components/input/input';

import { FormFields, FormFieldsNames } from '../../../../types';

export default function SecretAccessKey() {
  const { control } = useFormContext<FormFields>();
  return (
    <FormRow
      label={'Secret access key'}
      labelFor={FormFieldsNames.AWS_SECRET_ACCESSKEY}
    >
      <FormInput
        control={control}
        type={'password'}
        name={FormFieldsNames.AWS_SECRET_ACCESSKEY}
        details={'AWS account secret access key'}
        size={Size.L}
      />
    </FormRow>
  );
}
