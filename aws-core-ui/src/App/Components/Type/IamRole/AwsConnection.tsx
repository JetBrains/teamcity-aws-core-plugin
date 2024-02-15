import { React } from '@jetbrains/teamcity-api';
import {
  FormRow,
  FormSelect,
} from '@jetbrains-internal/tcci-react-ui-components';
import { useFormContext } from 'react-hook-form';
import { Size } from '@jetbrains/ring-ui/components/input/input';

import appStyles from '../../../styles.css';
import { FormFields, FormFieldsNames } from '../../../../types';

export default function AwsConnection() {
  const { control } = useFormContext<FormFields>();
  // TODO: use the correct data
  return (
    <FormRow
      label={'AWS Connection'}
      labelFor={FormFieldsNames.AWS_CONNECTION_ID}
    >
      <FormSelect
        control={control}
        name={FormFieldsNames.AWS_CONNECTION_ID}
        size={Size.L}
        popupClassName={appStyles.dropDownPopup}
      />
    </FormRow>
  );
}
