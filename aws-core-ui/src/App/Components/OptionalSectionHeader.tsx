import { React } from '@jetbrains/teamcity-api';
import {FormCheckbox, SectionHeader} from '@jetbrains-internal/tcci-react-ui-components';
import {useFormContext} from "react-hook-form";
import {FormFields, FormFieldsNames} from "../../types";

export default function OptionalSectionHeader({
  children,
}: {
  children: React.ReactNode;
}) {

    const { control } = useFormContext<FormFields>();

    return (
    <SectionHeader>
      <div style={{ display: 'flex', flexDirection: 'row', gap: '0.25rem' }}>
        <div>{children}</div>
        <div style={{ color: '#737577' }}>{'(optional)'}</div>

        <FormCheckbox name={FormFieldsNames.ALLOWED_IN_BUILDS_REQUEST}
                      label={'Available for builds'}
                      control={control} />
        <FormCheckbox name={FormFieldsNames.ALLOWED_IN_SUBPROJECTS}
                      label={'Available for sub-projects'}
                      control={control} />

      </div>
    </SectionHeader>
  );
}
