import { React } from '@jetbrains/teamcity-api';
import {
  FormInput,
  FormRow,
} from '@jetbrains-internal/tcci-react-ui-components';
import { useFormContext } from 'react-hook-form';
import { Size } from '@jetbrains/ring-ui/components/input/input';

import { FormFields, FormFieldsNames } from '../../../../types';

export default function SessionTag() {
  const { control } = useFormContext<FormFields>();
  return (
    <FormRow
      label={'Session tag'}
      labelFor={FormFieldsNames.AWS_IAM_ROLE_SESSION_NAME}
    >
      <FormInput
        control={control}
        name={FormFieldsNames.AWS_IAM_ROLE_SESSION_NAME}
        details={'Identifies which TeamCity connection assumes the role'}
        size={Size.L}
      />
    </FormRow>
  );
}
