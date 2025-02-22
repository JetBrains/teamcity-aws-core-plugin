import { React, utils } from '@jetbrains/teamcity-api';
import {
  errorMessage,
  HelpButton,
  useErrorService,
  useJspContainer,
  ReadOnlyContextProvider,
} from '@jetbrains-internal/tcci-react-ui-components';
import {
  ControlsHeight,
  ControlsHeightContext,
} from '@jetbrains/ring-ui/components/global/controls-height';
import { FormProvider } from 'react-hook-form';

import { SelectItem } from '@jetbrains/ring-ui/components/select/select';

import {
  Config,
  errorKeyToFieldNameConvertor,
  FormFields,
  FormFieldsNames,
  Mode,
  resolveHelpURL,
} from '../types';
import { SupportedProvidersContextProvider } from '../Contexts/SupportedProvidersContext';
import postConnection from '../Utilities/postConnection';
import { getErrorsFromResponseIfAny } from '../Utilities/responseParserUtils';
import useAwsConnectionForm from '../Hooks/useAwsConnectionForm';
import {
  ApplicationContextProvider,
  useApplicationContext,
} from '../Contexts/ApplicationContext';

import styles from './styles.css';
import { SupportedProviders } from './SupportedProviders';
import AwsDisplayName from './Components/AwsDisplayName';
import AwsConnectionId from './Components/AwsConnectionId';
import AwsRegion from './Components/AwsRegion';
import AwsType from './Components/AwsType';
import OptionalSectionHeader from './Components/OptionalSectionHeader';
import SwitchTypeContent from './SwitchTypeContent';
import ButtonControlPanel from './Components/ButtonControlPanel';
import ConnectionAvailabilitySettings from './Components/ConnectionAvailabilitySettings';

function redirectToDefaultPage(projectId: string) {
  document.location.href = `${utils.resolveRelativeURL(
    '/admin/editProject.html'
  )}?projectId=${projectId}&tab=oauthConnections`;
}

export function AppWrapper({ config }: { config: Config }) {
  const resetContainer = useJspContainer(
    'div.popupSaveButtonsBlock, div.modalDialogBody > table.runnerFormTable'
  );
  const doClose = React.useCallback(() => {
    redirectToDefaultPage(config.projectId);
  }, [config.projectId]);

  const doReset = React.useCallback(
    (ind: number, label: string) => {
      resetContainer();
      // @ts-ignore
      $('typeSelector').selectedIndex = ind;
      // @ts-ignore
      $('-ufd-teamcity-ui-typeSelector').value = label;
      // @ts-ignore
      BS.OAuthConnectionDialog.providerChanged($('typeSelector'));
    },
    [resetContainer]
  );

  const closeLink = document.querySelector(
    'div.OAuthConnectionDialog a.closeWindowLink'
  ) as HTMLLinkElement;

  if (closeLink !== null) {
    const oldOnClick = closeLink.onclick;
    closeLink.onclick = (e) => {
      resetContainer();
      oldOnClick?.call(closeLink, e);
    };
  }

  const newConf = { ...config, onClose: doClose };
  return (
    <ApplicationContextProvider config={newConf}>
      <ReadOnlyContextProvider value={config.readOnly}>
        <App doReset={doReset} />
      </ReadOnlyContextProvider>
    </ApplicationContextProvider>
  );
}

const formId = 'AwsConnectionsForm';

export function App({
  doReset = undefined,
  mode = Mode.DEFAULT,
}: {
  doReset?: (ind: number, label: string) => void;
  mode?: Mode;
}) {
  const { config } = useApplicationContext();
  const formMethods = useAwsConnectionForm(config);
  const { handleSubmit, setError, setValue } = formMethods;
  const { showErrorsOnForm, showErrorAlert } = useErrorService({
    setError,
    errorKeyToFieldNameConvertor,
  });

  const genericError = React.useCallback(
    (err: unknown) => {
      showErrorAlert(errorMessage(err));
    },
    [showErrorAlert]
  );

  const doClose = React.useCallback(() => {
    if (config.onClose) {
      config.onClose();
    } else {
      redirectToDefaultPage(config.projectId);
    }
  }, [config]);

  const onSubmit = React.useCallback(
    async (data: FormFields, event?: React.BaseSyntheticEvent) => {
      let response: Document | undefined;
      let isError = false;

      try {
        //if connectionId is set in config, then connection exists (hence we're not creating a new one)
        //otherwise put the value from connection id into the id field and nil the connection id field (to be safe)
        //awsConnectionId value is an entirely different thing, for the IAM connection mode
        if (config.connectionId === null || config.connectionId === '') {
          data[FormFieldsNames.ID] = data[FormFieldsNames.CONNECTION_ID];
          data[FormFieldsNames.CONNECTION_ID] = undefined;
        }

        const resp = await postConnection(config, data);
        response = new DOMParser().parseFromString(resp, 'text/xml');
        const errors = getErrorsFromResponseIfAny(response);

        if (errors) {
          isError = true;
          showErrorsOnForm(errors);
        }
      } catch (e) {
        isError = true;
        genericError(e);
      } finally {
        if (config.afterSubmit) {
          config.afterSubmit(data, isError, response, event);
        } else if (!isError) {
          doClose();
        }
      }
    },
    [config, doClose, genericError, showErrorsOnForm]
  );

  React.useEffect(() => {
    const dialog = document.getElementById('OAuthConnectionDialog');

    if (dialog) {
      dialog.style.top = '5%';
      dialog.style.position = 'fixed';
    }
  }, []); // fire once

  const providerDisplayed = mode === Mode.DEFAULT;

  const handleRegionChange = React.useCallback(
    (selected: SelectItem | null) => {
      if (selected!) {
        const key = selected?.key;
        let newValue;
        if (key.toString().startsWith('cn')) {
          newValue = `https://sts.${key}.amazonaws.com.cn`;
        } else {
          newValue = `https://sts.${key}.amazonaws.com`;
        }
        setValue(FormFieldsNames.AWS_STS_ENDPOINT, newValue);
      }
    },
    [setValue]
  );
  return (
    <SupportedProvidersContextProvider>
      <ControlsHeightContext.Provider value={ControlsHeight.S}>
        <FormProvider {...formMethods}>
          <form
            onSubmit={handleSubmit(onSubmit)}
            autoComplete="off"
            className={styles.App}
            id={formId}
          >
            <section>
              {providerDisplayed && <SupportedProviders reset={doReset} />}
              <AwsConnectionNote />
              <AwsDisplayName genericErrorHandler={genericError} />
              <AwsConnectionId />
              <AwsRegion onChangeCallback={handleRegionChange} />
              <AwsType />
            </section>
            <SwitchTypeContent config={config} />
            <section>
              <OptionalSectionHeader>{'Security'}</OptionalSectionHeader>
              <ConnectionAvailabilitySettings />
            </section>
            <ButtonControlPanel
              onClose={doClose}
              genericErrorHandler={genericError}
              mode={mode}
            />
          </form>
        </FormProvider>
      </ControlsHeightContext.Provider>
    </SupportedProvidersContextProvider>
  );
}

function AwsConnectionNote() {
  return (
    <div className={styles.note}>
      <span className={styles.commentary}>
        {'Connection that allows TeamCity to store and manage AWS Credentials'}
      </span>
      <HelpButton
        href={resolveHelpURL('?configuring-connections#AmazonWebServices')}
      />
    </div>
  );
}
