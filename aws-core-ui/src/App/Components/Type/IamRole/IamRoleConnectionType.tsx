import { React } from '@jetbrains/teamcity-api';
import { SectionHeader } from '@jetbrains-internal/tcci-react-ui-components';

import OptionalSectionHeader from '../../OptionalSectionHeader';
import StsEndpoint from '../StsEndpoint';

import SessionTag from './SessionTag';
import RoleArn from './RoleArn';
import AwsConnection from './AwsConnection';

export default function IamRoleConnectionType() {
  return (
    <>
      <section>
        <SectionHeader>{'IAM role'}</SectionHeader>
        <AwsConnection />
        <RoleArn />
      </section>
      <section>
        <OptionalSectionHeader>{'Session settings'}</OptionalSectionHeader>
        <SessionTag />
        <StsEndpoint />
      </section>
    </>
  );
}
