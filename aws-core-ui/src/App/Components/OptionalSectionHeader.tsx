import { React } from '@jetbrains/teamcity-api';
import { SectionHeader } from '@jetbrains-internal/tcci-react-ui-components';

export default function OptionalSectionHeader({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SectionHeader>
      <div style={{ display: 'flex', flexDirection: 'row', gap: '0.25rem' }}>
        <div>{children}</div>
        <div style={{ color: '#737577' }}>{'(optional)'}</div>
      </div>
    </SectionHeader>
  );
}
