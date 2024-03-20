import { React } from '@jetbrains/teamcity-api';
import {SectionHeader} from '@jetbrains-internal/tcci-react-ui-components';
import {useFormContext} from "react-hook-form";
import {FormFields} from "../../types";

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
      </div>
    </SectionHeader>
  );
}
