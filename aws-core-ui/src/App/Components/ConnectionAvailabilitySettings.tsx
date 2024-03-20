import {FormFields, FormFieldsNames} from "../../types";
import {FormCheckbox} from "@jetbrains-internal/tcci-react-ui-components";
import {useFormContext} from "react-hook-form";
import {React} from "@jetbrains/teamcity-api";


export default function ConnectionAvailabilitySettings() {
    const {control} = useFormContext<FormFields>();

    return (
        <>
            <FormCheckbox name={FormFieldsNames.ALLOWED_IN_BUILDS_REQUEST}
                        label={'Available for builds'}
                        control={control}/>
            <FormCheckbox name={FormFieldsNames.ALLOWED_IN_SUBPROJECTS}
                        label={'Available for sub-projects'}
                        control={control}/>
        </>
    );
}