import { React } from '@jetbrains/teamcity-api';

import { useApplicationContext } from '../../../../Contexts/ApplicationContext';
import { resolveHelpURL } from '../../../../types';

import styles from './styles.css';

const docUrl = resolveHelpURL('configuring-connections#AmazonWebServices');
export default function DefaultConnectionType() {
  const { config } = useApplicationContext();

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
        <a href={docUrl}>{'documentation'}</a>
      </div>
    );
  }
}
