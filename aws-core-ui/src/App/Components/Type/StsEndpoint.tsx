import { React } from '@jetbrains/teamcity-api';
import { FormInput, Label } from '@jetbrains-internal/tcci-react-ui-components';
import { useFormContext } from 'react-hook-form';

import { FormFields, FormFieldsNames } from '../../../types';
import styles from '../../styles.css';

export default function StsEndpoint() {
  const { control } = useFormContext<FormFields>();

  return (
    <div className={styles.rowStyle}>
      <Label>{'STS endpoint'}</Label>
      <FormInput
        control={control}
        name={FormFieldsNames.AWS_STS_ENDPOINT}
        details={'The global endpoint is: https://sts.amazonaws.com'}
      />
    </div>
  );
}
