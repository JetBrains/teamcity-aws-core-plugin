import { React } from '@jetbrains/teamcity-api';
import { FormInput, Label } from '@jetbrains-internal/tcci-react-ui-components';

import { useFormContext } from 'react-hook-form';

import { FormFields, FormFieldsNames } from '../../../../types';
import styles from '../../../styles.css';

export default function RoleArn() {
  const { control } = useFormContext<FormFields>();
  return (
    <div className={styles.rowStyle}>
      <Label>{'Role ARN'}</Label>
      <FormInput
        control={control}
        name={FormFieldsNames.AWS_IAM_ROLE_ARN}
        details={'Pre-configured IAM role with necessary permissions'}
      />
    </div>
  );
}
