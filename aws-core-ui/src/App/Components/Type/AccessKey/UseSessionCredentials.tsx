import {React} from '@jetbrains/teamcity-api';
import {FormCheckbox, FormRow, Label} from '@jetbrains-internal/tcci-react-ui-components';
import {useFormContext} from 'react-hook-form';

import {FormFields, FormFieldsNames} from '../../../../types';

export default function UseSessionCredentials() {
    const {control} = useFormContext<FormFields>();

    // TODO: the label 'Use session credentials' required on the right side as well
    return (
        <>
            <Label>{'Use session credentials'}</Label>
            <FormCheckbox
                name={FormFieldsNames.AWS_SESSION_CREDENTIALS}
                label={'Issue temporary credentials by request'}
                control={control}/>
        </>
    );
}
