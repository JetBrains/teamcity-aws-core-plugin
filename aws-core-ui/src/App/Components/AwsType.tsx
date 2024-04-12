import {React} from '@jetbrains/teamcity-api';
import {
    FormSelect, Label,
    Option,
} from '@jetbrains-internal/tcci-react-ui-components';

import {useFormContext} from 'react-hook-form';

import {FormFields, FormFieldsNames} from '../../types';

import appStyles from '../styles.css';

export enum AwsCredentialsType {
    ACCESS_KEYS = 'awsAccessKeys',
    IAM_ROLE = 'awsAssumeIamRole',
    DEFAULT_PROVIDER = 'defaultProvider',
}

export const credentialsTypeOptions: Option[] = [
    {key: AwsCredentialsType.ACCESS_KEYS, label: 'Access keys'},
    {key: AwsCredentialsType.IAM_ROLE, label: 'IAM role'},
    {
        key: AwsCredentialsType.DEFAULT_PROVIDER,
        label: 'Default Credential Provider Chain',
    },
];

export default function AwsType() {
    const {control} = useFormContext<FormFields>();
    return (
        <div className={appStyles.rowStyle}>
            <Label>
                {'Type'}
            </Label>
            <FormSelect
                name={FormFieldsNames.AWS_CREDENTIALS_TYPE}
                control={control}
                popupClassName={appStyles.dropDownPopup}
                data={credentialsTypeOptions}
                details={' '}
            />
        </div>
    );
}
