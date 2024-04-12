import {React} from '@jetbrains/teamcity-api';
import {useFormContext} from 'react-hook-form';
import {
    FormInput,
    Label,
} from '@jetbrains-internal/tcci-react-ui-components';

import {FormFields, FormFieldsNames} from '../../../../types';
import styles from '../../../styles.css'

export default function SecretAccessKey() {
    const {control} = useFormContext<FormFields>();
    return (
        <div className={styles.rowStyle}>
            <Label>{'Secret access key'}</Label>

            <FormInput
                control={control}
                type={'password'}
                name={FormFieldsNames.AWS_SECRET_ACCESSKEY}
                details={'AWS account secret access key'}
            />
        </div>
    );
}
