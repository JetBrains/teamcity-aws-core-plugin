import { React } from '@jetbrains/teamcity-api';
import { useFormContext } from 'react-hook-form';
import {
  FormInput,
  FormRow,
} from '@jetbrains-internal/tcci-react-ui-components';
import { Size } from '@jetbrains/ring-ui/components/input/input';

import { FormFields, FormFieldsNames } from '../../../../types';

export default function AccessKeyId() {
  const { control } = useFormContext<FormFields>();
  return (
    <FormRow
      label={'Access key ID'}
      labelFor={FormFieldsNames.AWS_ACCESSKEY_ID}
    >
      <FormInput
        control={control}
        name={FormFieldsNames.AWS_ACCESSKEY_ID}
        details={'AWS account access key ID'}
        size={Size.L}
      />
    </FormRow>
  );
}
