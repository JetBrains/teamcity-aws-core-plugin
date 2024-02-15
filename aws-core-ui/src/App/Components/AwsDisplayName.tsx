import { React } from '@jetbrains/teamcity-api';
import {
  FormInput,
  FormRow,
} from '@jetbrains-internal/tcci-react-ui-components';
import { Size } from '@jetbrains/ring-ui/components/input/input';
import { useFormContext } from 'react-hook-form';

import { FormFields, FormFieldsNames } from '../../types';
import { useApplicationContext } from '../../Contexts/ApplicationContext';
import getGeneratedId from '../../Utilities/getGeneratedId';

export default function AwsDisplayName({
  genericErrorHandler,
}: {
  genericErrorHandler: (error: unknown) => void;
}) {
  const [triggeredState, setTriggeredState] = React.useState(false);
  const { config, isCreateMode } = useApplicationContext();
  const { control, setValue, getValues } = useFormContext<FormFields>();

  const displayNameChanged = React.useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      if (isCreateMode) {
        const newValue = event.target.value;
        getGeneratedId(newValue, config.projectId)
          .then((connectionId: string) =>
            setValue(FormFieldsNames.CONNECTION_ID, connectionId)
          )
          .catch(genericErrorHandler);
      }
    },
    [config.projectId, genericErrorHandler, isCreateMode, setValue]
  );

  React.useEffect(() => {
    if (triggeredState) {
      return;
    }

    const displayName = getValues(FormFieldsNames.DISPLAY_NAME);
    if (isCreateMode && displayName) {
      getGeneratedId(displayName, config.projectId)
        .then((connectionId: string) =>
          setValue(FormFieldsNames.CONNECTION_ID, connectionId)
        )
        .finally(() => setTriggeredState(true));
    }
  }, [config.projectId, getValues, isCreateMode, setValue, triggeredState]);

  return (
    <FormRow label="Display name" labelFor={FormFieldsNames.DISPLAY_NAME}>
      <FormInput
        name={FormFieldsNames.DISPLAY_NAME}
        control={control}
        size={Size.L}
        details={'Provide a name to distinguish this connection from others'}
        rules={{
          required: 'Display name is mandatory',
          onChange: displayNameChanged,
        }}
      />
    </FormRow>
  );
}
