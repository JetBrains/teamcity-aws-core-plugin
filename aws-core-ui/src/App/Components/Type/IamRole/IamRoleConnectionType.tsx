import { React } from '@jetbrains/teamcity-api';
import { SectionHeader } from '@jetbrains-internal/tcci-react-ui-components';

import { useFormContext } from 'react-hook-form';

import StsEndpoint from '../StsEndpoint';

import {
  AvailableConnectionsData,
  Config,
  FormFieldsNames,
} from '../../../../types';

import SessionTag from './SessionTag';
import RoleArn from './RoleArn';
import AwsConnection from './AwsConnection';
import ExternalID from './ExternalID';

export default function IamRoleConnectionType({ config }: { config: Config }) {
  const ctx = useFormContext();
  const data = {
    awsConnectionId: config.awsConnectionId,
    awsConnectionFormFieldName: FormFieldsNames.AWS_CONNECTION_ID,
    projectId: config.projectId,
    availableConnectionsResource:
      config.availableAwsConnectionsControllerResource,
    availableConnectionsControllerUrl:
      config.availableAwsConnectionsControllerUrl,
    awsConnectionTypesFilter: config.awsConnectionTypesFilter,
  } as AvailableConnectionsData;

  return (
    <>
      <section>
        <SectionHeader>{'IAM Role'}</SectionHeader>
        <AwsConnection data={data} ctx={ctx} />
        <RoleArn />
        {data.awsConnectionId && <ExternalID config={config} />}
      </section>
      <section>
        <SectionHeader>{'Session settings'}</SectionHeader>
        <SessionTag />
        <StsEndpoint />
      </section>
    </>
  );
}
