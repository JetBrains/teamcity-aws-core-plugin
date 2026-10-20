/*
 * Copyright 2000-2024 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { React } from '@jetbrains/teamcity-api';

import { UseFormReturn } from 'react-hook-form';

import { AvailableAwsConnectionsData, AwsConnectionData } from '../components';

import AwsConnection from './Components/Type/IamRole/AwsConnection';
import AwsConnectionsControls from './AwsConnectionsControls';

import styles from './styles.css';

export default function AwsConnectionsWithButtons({
  ctx,
  connectionsData,
  formFieldName,
  awsConnectionsStyle = undefined,
}: {
  ctx: UseFormReturn;
  connectionsData: AwsConnectionData;
  formFieldName: string;
  awsConnectionsStyle?: string;
}) {
  const [currentConnection, setCurrentConnection] = React.useState(
    connectionsData.awsConnectionId
  );
  const availableConnectionsData = {
    awsConnectionId: currentConnection,
    awsConnectionFormFieldName: formFieldName,
    projectId: connectionsData.projectId,
    awsConnectionsStyle,
    availableConnectionsControllerUrl:
      connectionsData.awsAvailableConnectionsControllerUrl,
    availableConnectionsResource:
      connectionsData.awsAvailableConnectionsResource,
    awsConnectionTypesFilter: connectionsData.awsConnectionTypesFilter,
  } as AvailableAwsConnectionsData;
  const onConnectionSelected = (connectionId: string) =>
    setCurrentConnection(connectionId);

  return (
    <div className={styles.connectionControlPanel}>
      <AwsConnection
        data={availableConnectionsData}
        ctx={ctx}
        onConnectionSelected={onConnectionSelected}
      />

      <AwsConnectionsControls
        currentConnection={currentConnection}
        connectionData={connectionsData}
        onCreated={onConnectionSelected}
      />
    </div>
  );
}
