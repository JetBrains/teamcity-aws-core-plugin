import {React} from '@jetbrains/teamcity-api';
import {
    FormSelect, Label,
} from '@jetbrains-internal/tcci-react-ui-components';
import {UseFormReturn} from 'react-hook-form';

import appStyles from '../../../styles.css';
import useAwsConnections from "../../../../Hooks/useAwsConnections";
import styles from '../../../styles.css';
import {AvailableConnectionsData} from "../../../../types";

export default function AwsConnection(
    {
        data,
        ctx,
        onConnectionSelected = undefined,
    }: {
        data: AvailableConnectionsData,
        ctx: UseFormReturn,
        onConnectionSelected?: (connId: string) => void
    }) {

    const {control, setError, clearErrors, setValue} = ctx;
    const {connectionOptions, error, isLoading, reloadConnectionOptions} =
        useAwsConnections(data);

    const handleSelection = React.useCallback(
        (event: any) => {
            const id = event?.key;
            if (onConnectionSelected) {
                onConnectionSelected(id);
            }
            clearErrors();
        },
        [clearErrors]
    );

    React.useEffect(() => {
        if (data.awsConnectionId && connectionOptions) {
            const conn = connectionOptions.find(
                ({key}) => key === data.awsConnectionId
            );

            if (conn) {
                setValue(data.awsConnectionFormFieldName, conn);
            }
        }
    }, [connectionOptions])

    React.useEffect(() => {
        if (error) {
            setError(data.awsConnectionFormFieldName, {
                message: error,
                type: 'custom',
            });
        } else {
            clearErrors(data.awsConnectionFormFieldName);
        }
    }, [clearErrors, error, setError]);

    const style = !!data.awsConnectionsStyle && data.awsConnectionsStyle !== '' ? data.awsConnectionsStyle : styles.rowStyle;

    return (
        <div className={style}>
            <Label>
                {'AWS Connection'}
            </Label>
            <FormSelect
                control={control}
                name={data.awsConnectionFormFieldName}
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
