import { React } from '@jetbrains/teamcity-api';
import { createRoot } from 'react-dom/client';

import { Config } from './types';
import { AppWrapper } from './App/App';
import './styles.css';

global.renderEditAwsConnection = (config: Config) => {
  const container = document.getElementById('edit-aws-connection-root');

  if (!container) {
    throw new Error('No container found');
  }

  const root = createRoot(container!);
  root.render(<AppWrapper config={config} />);
};
