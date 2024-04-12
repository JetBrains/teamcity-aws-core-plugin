import {React} from '@jetbrains/teamcity-api';
import {
    FormInput,
    Label,
} from '@jetbrains-internal/tcci-react-ui-components';
import {useFormContext} from 'react-hook-form';
import styles from '../../../styles.css';


import {FormFields, FormFieldsNames} from '../../../../types';

export default function SessionTag() {
    const {control} = useFormContext<FormFields>();
    return (
        <div className={styles.rowStyle}>
            <Label>
                {'Session tag'}
            </Label>
            <FormInput
                control={control}
                name={FormFieldsNames.AWS_IAM_ROLE_SESSION_NAME}
                details={'Identifies which TeamCity connection assumes the role'}
            />
        </div>
    );
}
