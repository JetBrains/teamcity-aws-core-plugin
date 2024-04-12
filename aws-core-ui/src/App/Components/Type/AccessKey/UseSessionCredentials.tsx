import {React} from '@jetbrains/teamcity-api';
import {FormCheckbox, Label} from '@jetbrains-internal/tcci-react-ui-components';
import {useFormContext} from 'react-hook-form';

import {FormFields, FormFieldsNames} from '../../../../types';
import styles from '../../../styles.css'

export default function UseSessionCredentials() {
    const {control} = useFormContext<FormFields>();

    return (
        <div className={styles.rowStyle}>
            <Label>{'Use session credentials'}</Label>
            <FormCheckbox
                name={FormFieldsNames.AWS_SESSION_CREDENTIALS}
                label={'Issue temporary credentials by request'}
                control={control}/>
        </div>
    );
}
