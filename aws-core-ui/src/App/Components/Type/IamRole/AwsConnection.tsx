import {React} from '@jetbrains/teamcity-api';
import {
    FormSelect, Label, Option,
} from '@jetbrains-internal/tcci-react-ui-components';
import {useFormContext} from 'react-hook-form';

import appStyles from '../../../styles.css';
import {FormFields, FormFieldsNames} from '../../../../types';
import useAwsConnections from "../../../../Hooks/useAwsConnections";
import {useApplicationContext} from "../../../../Contexts/ApplicationContext";
import styles from '../../../styles.css';

export default function AwsConnection() {
    const ctx = useApplicationContext();
    const {control, setError, clearErrors, getValues, setValue} = useFormContext<FormFields>();
    const {connectionOptions, error, isLoading, reloadConnectionOptions} =
        useAwsConnections();

    const [, setCurrentConnectionId] = React.useState(
        (getValues(FormFieldsNames.AWS_CONNECTION_ID) as Option)?.key
    );

    const handleSelection = React.useCallback(
        (event: any) => {
            setCurrentConnectionId(event?.key?.id);
            clearErrors();
        },
        [clearErrors]
    );

    React.useEffect(() => {
        if (ctx.config.awsConnectionId && connectionOptions) {
            const conn = connectionOptions.find(
                ({key}) => key === ctx.config.awsConnectionId
            );

            if (conn) {
                setValue(FormFieldsNames.AWS_CONNECTION_ID, conn);
            }
        }
    }, [connectionOptions])

    React.useEffect(() => {
        if (error) {
            setError(FormFieldsNames.AWS_CONNECTION_ID, {
                message: error,
                type: 'custom',
            });
        } else {
            clearErrors(FormFieldsNames.AWS_CONNECTION_ID);
        }
    }, [clearErrors, error, setError]);

    return (
        <div className={styles.rowStyle}>
            <Label>
                {'AWS Connection'}
            </Label>
            <FormSelect
                control={control}
                name={FormFieldsNames.AWS_CONNECTION_ID}
                data={connectionOptions}
                onBeforeOpen={reloadConnectionOptions}
                onSelect={handleSelection}
                details={' '}
                popupClassName={appStyles.dropDownPopup}
                loading={isLoading}
            />
        </div>
    );
}
