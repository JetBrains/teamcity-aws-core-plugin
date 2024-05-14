import {React} from '@jetbrains/teamcity-api';
import {SectionHeader} from '@jetbrains-internal/tcci-react-ui-components';

import OptionalSectionHeader from '../../OptionalSectionHeader';
import StsEndpoint from '../StsEndpoint';

import SessionTag from './SessionTag';
import RoleArn from './RoleArn';
import AwsConnection from './AwsConnection';
import {AvailableConnectionsData, Config, FormFieldsNames} from "../../../../types";
import {useFormContext} from "react-hook-form";

export default function IamRoleConnectionType({config}: {config: Config}) {
    const ctx = useFormContext();
    const data = {
        awsConnectionId: config.awsConnectionId,
        awsConnectionFormFieldName: FormFieldsNames.AWS_CONNECTION_ID,
        projectId: config.projectId,
        availableConnectionsResource: config.availableAwsConnectionsControllerResource,
        availableConnectionsControllerUrl: config.availableAwsConnectionsControllerUrl
    } as AvailableConnectionsData;

    return (
    <>
      <section>
        <SectionHeader>{'IAM role'}</SectionHeader>
          <AwsConnection data={data}
                         ctx={ctx}/>
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
