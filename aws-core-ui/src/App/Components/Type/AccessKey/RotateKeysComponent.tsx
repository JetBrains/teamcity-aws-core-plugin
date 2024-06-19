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

import Button from '@jetbrains/ring-ui/components/button/button';
import React, { useState } from 'react';

import Icon, { Color } from '@jetbrains/ring-ui/components/icon';

import okIcon from '@jetbrains/icons/ok';

import styles from '../../../styles.css';

import { requestKeyRotation } from '../../../../Utilities/rotateKeys';
import { useApplicationContext } from '../../../../Contexts/ApplicationContext';

export default function RotateKeysComponent() {
  const { config, setConfig } = useApplicationContext();
  const [isLoading, setLoading] = useState(false);
  const [rotationStatus, setRotationStatus] = useState({} as RotationStatus);
  const updateConfig = (key: string, secret: string) => {
    const newConfig = { ...config, accessKeyId: key, secretAccessKey: secret };
    setConfig(newConfig);
  };

  const rotateKeys = () => {
    setLoading(true);
    requestKeyRotation(config)
      .then((res) => {
        if (res.errorMessage) {
          setRotationStatus({
            success: false,
            message: res.errorMessage,
          });
        } else {
          updateConfig(res.key, res.secret);
          setRotationStatus({
            success: true,
            message: 'Keys have been rotated',
          });
        }
      })
      .catch((e) =>
        console.error('An unexpected key rotation process exception: ', e)
      )
      .finally(() => setLoading(false));
  };

  return (
    <div className={styles.rowStyle}>
      <Button loader={isLoading} onClick={rotateKeys}>
        {'Rotate keys'}
      </Button>
      <div className={styles.rotateKeyMessage}>
        {' '}
        <RotationStatusMessage status={rotationStatus} />{' '}
      </div>
    </div>
  );
}

function RotationStatusMessage({ status }: { status: RotationStatus }) {
  return status.success ? (
    <div className={styles.successSimple}>
      <Icon glyph={okIcon} color={Color.GREEN} className={styles.successIcon} />
      {status.message}
    </div>
  ) : (
    <div className={styles.error}>{status.message}</div>
  );
}

interface RotationStatus {
  success: boolean;
  message?: string;
}
