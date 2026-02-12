import Dialog from '@jetbrains/ring-ui/components/dialog/dialog';
import { Content, Header } from '@jetbrains/ring-ui/components/island/island';
import Panel from '@jetbrains/ring-ui/components/panel/panel';
import Button from '@jetbrains/ring-ui/components/button/button';
import { React } from '@jetbrains/teamcity-api';
import ButtonSet from '@jetbrains/ring-ui/components/button-set/button-set';

import styles from '../styles.css';
import { useApplicationContext } from '../../Contexts/ApplicationContext';

export interface ConfirmDialogProps {
  show?: boolean;
  onDelete?: () => void;
  onCancel?: () => void;
}

export default function RotateConfirmationDialog({
  show,
  onDelete,
  onCancel,
}: ConfirmDialogProps) {
  const { config } = useApplicationContext();
  return (
    <Dialog
      show={show}
      dense
      trapFocus
      autoFocusFirst
      className={styles.fixDialog}
    >
      <Header>{'Delete Keys'}</Header>
      <Content>
        <div>
          {`Are you sure you want to rotate the access key? The previous key will be deleted in ${config.oldKeyPreserveTime}.`}
        </div>
      </Content>
      <Panel>
        <ButtonSet>
          <Button danger onClick={onDelete}>
            {'Delete'}
          </Button>
          <Button onClick={onCancel}>{'Cancel'}</Button>
        </ButtonSet>
      </Panel>
    </Dialog>
  );
}
