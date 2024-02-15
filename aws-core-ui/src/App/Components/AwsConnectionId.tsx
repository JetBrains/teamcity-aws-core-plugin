import { React } from '@jetbrains/teamcity-api';
import { Size } from '@jetbrains/ring-ui/components/input/input';

import {
  FormInput,
  FormRow,
} from '@jetbrains-internal/tcci-react-ui-components';

import { useFormContext } from 'react-hook-form';

import { FormFields, FormFieldsNames } from '../../types';
import { useApplicationContext } from '../../Contexts/ApplicationContext';

export default function AwsConnectionId() {
  const { isCreateMode } = useApplicationContext();
  const { control } = useFormContext<FormFields>();

  return (
    <FormRow label="Connection ID" labelFor={FormFieldsNames.CONNECTION_ID}>
      <FormInput
        name={FormFieldsNames.CONNECTION_ID}
        control={control}
        disabled={!isCreateMode}
        size={Size.L}
        rules={{ required: 'Connection ID is mandatory' }}
        details={
          'This ID is used in URLs, REST API, HTTP requests to the server and configuration settings in the TeamCity Data Directory.'
        }
      />
    </FormRow>
  );
}
