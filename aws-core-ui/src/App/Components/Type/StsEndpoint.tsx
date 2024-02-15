import { React } from '@jetbrains/teamcity-api';
import {
  FormInput,
  FormRow,
} from '@jetbrains-internal/tcci-react-ui-components';
import { useFormContext } from 'react-hook-form';
import { Size } from '@jetbrains/ring-ui/components/input/input';

import { FormFields, FormFieldsNames } from '../../../types';

export default function StsEndpoint() {
  const { control } = useFormContext<FormFields>();

  return (
    <FormRow label={'STS endpoint'} labelFor={FormFieldsNames.AWS_STS_ENDPOINT}>
      <FormInput
        control={control}
        size={Size.L}
        name={FormFieldsNames.AWS_STS_ENDPOINT}
        details={'The global endpoint is: https://sts.amazonaws.com'}
      />
    </FormRow>
  );
}
