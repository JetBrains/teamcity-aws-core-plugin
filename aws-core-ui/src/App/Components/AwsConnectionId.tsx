import {React} from '@jetbrains/teamcity-api';

import {
    FormInput,
    Label,
} from '@jetbrains-internal/tcci-react-ui-components';

import {useFormContext} from 'react-hook-form';

import {FormFields, FormFieldsNames} from '../../types';
import {useApplicationContext} from '../../Contexts/ApplicationContext';
import styles from '../styles.css';

export default function AwsConnectionId() {
    const {isCreateMode} = useApplicationContext();
    const {control} = useFormContext<FormFields>();

    return (
        <div className={styles.rowStyle}>
            <Label>
                {'Connection ID'}
            </Label>
            <FormInput
                name={FormFieldsNames.CONNECTION_ID}
                control={control}
                disabled={!isCreateMode}
                rules={{required: 'Connection ID is mandatory'}}
                details={
                    'This ID is used in URLs, REST API, HTTP requests to the server and configuration settings in the TeamCity Data Directory.'
                }
            />
        </div>
    );
}
