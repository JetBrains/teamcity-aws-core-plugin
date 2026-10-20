import {
  FormRow,
  FormSelect,
} from '@jetbrains-internal/tcci-react-ui-components';
import { React } from '@jetbrains/teamcity-api';
import { useFormContext } from 'react-hook-form';

import { SelectItem } from '@jetbrains/ring-ui/components/select/select';

import { Size } from '@jetbrains/ring-ui/components/input/input';

import { FormFields, FormFieldsNames } from '../types';
import { useSupportedProvidersContext } from '../Contexts/SupportedProvidersContext';

import { useApplicationContext } from '../Contexts/ApplicationContext';

import styles from './styles.css';

export function SupportedProviders({
  reset,
}: {
  reset?: (ind: number, label: string) => void;
}) {
  const { config, isEditMode } = useApplicationContext();
  const { control } = useFormContext<FormFields>();
  const { providers, loading, error } = useSupportedProvidersContext();

  const handleSelect = React.useCallback(
    (option: SelectItem | null) => {
      let ind = 0;
      const label = option?.label ?? '';
      if (option) {
        ind = providers.findIndex((e) => e.key === option.key);
        ind = ind < 0 ? 0 : ind;
      }
      reset?.(ind + 1, label.toString()); // +1 because of the first element is the default one, and it is not visible in the list
    },
    [providers, reset]
  );

  return (
    <FormRow label="Connection type:">
      <FormSelect
        name={FormFieldsNames.PROVIDER_TYPE}
        control={control}
        loading={loading}
        data={providers}
        error={error}
        size={Size.L}
        disabled={isEditMode || config.disableTypeSelection === true}
        popupClassName={styles.dropDownPopup}
        onSelect={handleSelect}
      />
    </FormRow>
  );
}
