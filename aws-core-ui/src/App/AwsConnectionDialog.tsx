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

import Dialog from '@jetbrains/ring-ui/components/dialog/dialog';

import { Header } from '@jetbrains/ring-ui/components/island/island';

import { React } from '@jetbrains/teamcity-api';

import { Config, Mode } from '../types';

import { App } from './App';
import styles from './styles.css';
import {ApplicationContextProvider} from "../Contexts/ApplicationContext";

export default function AwsConnectionDialog({
  config,
  active,
  mode,
}: {
  config: Config;
  active: boolean;
  mode: Mode;
}) {
  const edit = config.awsConnectionId !== '';

  return (
    <Dialog
      show={active}
      trapFocus
      autoFocusFirst
      className={styles.connectionsDialog}
    >
      <Header> {edit ? 'Edit AWS Connection' : 'Add AWS Connection'} </Header>
      <ApplicationContextProvider config={config} >
        <App mode={mode} />
      </ApplicationContextProvider>
    </Dialog>
  );
}
