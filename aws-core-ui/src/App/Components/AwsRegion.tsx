import { React } from '@jetbrains/teamcity-api';
import {
  FormSelect,
  Label,
  Option,
} from '@jetbrains-internal/tcci-react-ui-components';

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
    <div className={appStyles.rowStyle}>
      <Label>{'Aws region'}</Label>
      <FormSelect
        name={FormFieldsNames.AWS_REGION}
        control={control}
        details={'Select the region where this connection will be used'}
        data={regionOptions}
        popupClassName={appStyles.dropDownPopup}
      />
    </div>
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
