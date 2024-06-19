import { React } from '@jetbrains/teamcity-api';
import { SectionHeader } from '@jetbrains-internal/tcci-react-ui-components';

import OptionalSectionHeader from '../../OptionalSectionHeader';
import StsEndpoint from '../StsEndpoint';

import UseSessionCredentials from './UseSessionCredentials';
import AccessKeyId from './AccessKeyId';
import SecretAccessKey from './SecretAccessKey';
import RotateKeysComponent from './RotateKeysComponent';

export default function AccessKeysConnectionType() {
  return (
    <>
      <section>
        <SectionHeader>{'Access keys'}</SectionHeader>
        <AccessKeyId />
        <SecretAccessKey />
        <RotateKeysComponent />
      </section>
      <section>
        <OptionalSectionHeader>{'Session settings'}</OptionalSectionHeader>
        <UseSessionCredentials />
        <StsEndpoint />
      </section>
    </>
  );
}
