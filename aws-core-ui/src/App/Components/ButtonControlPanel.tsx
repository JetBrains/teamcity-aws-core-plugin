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
import Panel from '@jetbrains/ring-ui/components/panel/panel';
import ButtonSet from '@jetbrains/ring-ui/components/button-set/button-set';
import Button from '@jetbrains/ring-ui/components/button/button';
import Icon, { Color } from '@jetbrains/ring-ui/components/icon';

import { useFormContext } from 'react-hook-form';
import { useErrorService } from '@jetbrains-internal/tcci-react-ui-components';

import okIcon from '@jetbrains/icons/ok';

import { testAwsConnection } from '../../Utilities/testAwsConnection';
import { FormFields, Mode } from '../../types';
import styles from '../styles.css';

import { toRequestData } from '../../Utilities/postConnection';
import { useApplicationContext } from '../../Contexts/ApplicationContext';

import TestAwsConnectionDialog from './TestAwsConnectionDialog';

export default function ButtonControlPanel({
  onClose,
  genericErrorHandler,
  mode,
}: {
  onClose: () => void;
  genericErrorHandler: (error: unknown) => void;
  mode: Mode;
}) {
  const [testDialogActive, setTestDialogActive] = React.useState(false);
  const [testingConnection, setTestingConnection] = React.useState(false);
  const [connectionSuccessful, setConnectionSuccessful] = React.useState(false);
  const [testResultMessage, setTestResultMessage] = React.useState('');
  const ctx = useApplicationContext();
  const { clearAlerts } = useErrorService();

  const { getValues } = useFormContext<FormFields>();

  const testConnection = React.useCallback(async () => {
    const formData = getValues();
    clearAlerts();
    setConnectionSuccessful(false);
    setTestDialogActive(false);
    setTestingConnection(true);
    try {
      const result = await testAwsConnection(
        toRequestData(ctx.config, formData)
      );

      setConnectionSuccessful(result.success);
      setTestResultMessage(result.message);
      setTestDialogActive(true);
    } catch (e) {
      genericErrorHandler(e);
    } finally {
      setTestingConnection(false);
    }
  }, [clearAlerts, ctx.config, genericErrorHandler, getValues]);

  return (
    <Panel className={styles.awsConnectionButtonPanel}>
      <ButtonSet>
        <Button primary type="submit">
          {mode === Mode.CONVERT
            ? 'Convert'
            : ctx.isEditMode
            ? 'Save'
            : 'Create'}
        </Button>
        <Button onClick={() => onClose()}>{'Cancel'}</Button>
        <Button loader={testingConnection} onClick={testConnection}>
          {'Test Connection'}
        </Button>
      </ButtonSet>

      <TestAwsConnectionDialog
        active={testDialogActive}
        success={connectionSuccessful}
        testConnectionInfo={testResultMessage}
        onClose={() => setTestDialogActive(false)}
      />
    </Panel>
  );
}
