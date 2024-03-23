import {React} from '@jetbrains/teamcity-api';
import {
    FormRow,
    FormSelect,
} from '@jetbrains-internal/tcci-react-ui-components';
import {useFormContext} from 'react-hook-form';
import {Size} from '@jetbrains/ring-ui/components/input/input';

import appStyles from '../../../styles.css';
import {FormFields, FormFieldsNames} from '../../../../types';
import useAwsConnections from "../../../../Hooks/useAwsConnections";

export default function AwsConnection() {
    const {control, setError, clearErrors, getValues} = useFormContext<FormFields>();
    const {connectionOptions, error, isLoading, reloadConnectionOptions} =
        useAwsConnections();

    const [ , setCurrentConnectionId ] = React.useState(
        getValues(FormFieldsNames.AWS_CONNECTION_ID)?.key?.id
    );

    const handleSelection = React.useCallback(
        (event: any) => {
            setCurrentConnectionId(event?.key?.id);
            clearErrors();
        },
        [clearErrors]
    );

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
        <FormRow
            label={'AWS Connection'}
            labelFor={FormFieldsNames.AWS_CONNECTION_ID}
        >
            <FormSelect
                control={control}
                name={FormFieldsNames.AWS_CONNECTION_ID}
                size={Size.L}
                data={connectionOptions}
                onBeforeOpen={reloadConnectionOptions}
                onSelect={handleSelection}
                popupClassName={appStyles.dropDownPopup}
                loading={isLoading}
            />
        </FormRow>
    );
}
