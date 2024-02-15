import { React } from '@jetbrains/teamcity-api';
import {
  FormRow,
  FormSelect,
  Option,
} from '@jetbrains-internal/tcci-react-ui-components';

import { Size } from '@jetbrains/ring-ui/components/input/input';

import { useFormContext } from 'react-hook-form';

import { Config, FormFields, FormFieldsNames } from '../../types';
import { useApplicationContext } from '../../Contexts/ApplicationContext';

import useDefaultSplitter from '../../Hooks/useDefaultSplitter';

import appStyles from '../styles.css';

export default function AwsRegion() {
  const { config } = useApplicationContext();
  const { control } = useFormContext<FormFields>();
  const regionOptions = useRegionOptions(config);

  return (
    <FormRow label={'Aws region'} labelFor={FormFieldsNames.AWS_REGION}>
      <FormSelect
        name={FormFieldsNames.AWS_REGION}
        control={control}
        size={Size.L}
        details={'Select the region where this connection will be used'}
        data={regionOptions}
        popupClassName={appStyles.dropDownPopup}
      />
    </FormRow>
  );
}

export function useRegionOptions(config: Config): Option[] {
  const allRegionKeysArray = useDefaultSplitter(
    config.allRegions.allRegionKeys
  );
  const allRegionNamesArray = useDefaultSplitter(
    config.allRegions.allRegionValues
  );

  return (
    allRegionKeysArray.map((key, index) => ({
      key,
      label: allRegionNamesArray[index],
    })) ?? []
  );
}
