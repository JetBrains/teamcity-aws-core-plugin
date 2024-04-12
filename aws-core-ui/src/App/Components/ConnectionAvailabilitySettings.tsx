import {FormFields, FormFieldsNames} from "../../types";
import {FormCheckbox, Label} from "@jetbrains-internal/tcci-react-ui-components";
import {useFormContext} from "react-hook-form";
import {React} from "@jetbrains/teamcity-api";
import styles from '../styles.css';


export default function ConnectionAvailabilitySettings() {
    const {control} = useFormContext<FormFields>();

    return (
        <>
            <div className={styles.rowStyle}>
            <Label>{'Available for builds'}</Label>
            <FormCheckbox name={FormFieldsNames.ALLOWED_IN_BUILDS_REQUEST}
                          label={'Project build steps can utilize the connection'}
                          control={control}/>
            </div>

            <div className={styles.rowStyle}>
            <Label>{'Available for sub-projects'}</Label>
            <FormCheckbox name={FormFieldsNames.ALLOWED_IN_SUBPROJECTS}
                          label={'Sub-projects can utilize the connection'}
                          control={control}/>
            </div>
        </>
    );
}