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
import {
  Label,
  FormButton,
} from '@jetbrains-internal/tcci-react-ui-components';

import { useFormContext } from 'react-hook-form';

import copyIcon from '@jetbrains/icons/copy';

import alertService from '@jetbrains/ring-ui/components/alert-service/alert-service';

import { Config, FormFieldsNames } from '../../../../types';
import styles from '../../../styles.css';
import useExternalIds from '../../../../Hooks/useExternalIds';

import { copyClipboard } from '../../../../Utilities/clipboard';

export default function ExternalID({ config }: { config: Config }) {
  const { control, setError, clearErrors } = useFormContext();
  const { externalId, error, isLoading } = useExternalIds(config);
  const copyData = React.useCallback(async () => {
    try {
      await copyClipboard(externalId);
      alertService.message('Copied!', 5);
    } catch {
      alertService.error('Could not copy external ID', 5);
    }
  }, [externalId]);
  React.useEffect(() => {
    if (error) {
      setError(FormFieldsNames.AWS_IAM_ROLE_EXTERNAL_ID, {
        message: error,
        type: 'custom',
      });
    } else {
      clearErrors(FormFieldsNames.AWS_IAM_ROLE_EXTERNAL_ID);
    }
  }, [error, clearErrors, setError]);
  return (
    <div className={styles.rowStyle}>
      <Label>{'External ID'}</Label>
      <FormButton
        control={control}
        name={FormFieldsNames.AWS_IAM_ROLE_EXTERNAL_ID}
        details={
          'External ID is strongly recommended to be used in role trust relationship condition'
        }
        icon={copyIcon}
        label={externalId}
        loader={isLoading}
        text
        onClick={copyData}
      />
    </div>
  );
}
