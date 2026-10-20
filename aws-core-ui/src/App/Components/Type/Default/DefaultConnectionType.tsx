import { React } from '@jetbrains/teamcity-api';

import { useApplicationContext } from '../../../../Contexts/ApplicationContext';
import { resolveHelpURL } from '../../../../types';

import styles from './styles.css';

const docUrl = resolveHelpURL('?configuring-connections#AmazonWebServices');
export default function DefaultConnectionType() {
  const { config } = useApplicationContext();

  const helpOnClick = React.useCallback(
    (event: React.MouseEvent<HTMLAnchorElement>) => {
      if (docUrl) {
        window?.BS?.Util?.showHelp(event, docUrl, {
          width: 0,
          height: 0,
        });
        event.preventDefault();
      }
      return false;
    },
    []
  );

  if (config.isDefaultCredProviderEnabled) {
    return <div />;
  } else {
    return (
      <div className={styles.errorText}>
        {'The '}
        <b>{'Default Credentials Provider Chain'}</b>
        {
          ' type is disabled on this server. For instructions on how to enable it and for more information see the '
        }
        <a href={docUrl} onClick={helpOnClick}>
          {'documentation'}
        </a>
      </div>
    );
  }
}
