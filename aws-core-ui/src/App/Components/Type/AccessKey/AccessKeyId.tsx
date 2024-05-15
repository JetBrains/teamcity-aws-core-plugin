import { React } from '@jetbrains/teamcity-api';
import { useFormContext } from 'react-hook-form';
import { FormInput, Label } from '@jetbrains-internal/tcci-react-ui-components';

import { FormFields, FormFieldsNames } from '../../../../types';

import styles from '../../../styles.css';

export default function AccessKeyId() {
  const { control } = useFormContext<FormFields>();
  return (
    <div className={styles.rowStyle}>
      <Label>{'Access key ID'}</Label>
      <FormInput
        control={control}
        name={FormFieldsNames.AWS_ACCESSKEY_ID}
        details={'AWS account access key ID'}
      />
    </div>
  );
}
