import { React } from '@jetbrains/teamcity-api';

import { ConfirmDialogProps } from '../App/Components/RotateConfirmationDialog';

export type RotationResolverType = { isOk: boolean };
export type RotationContextType = {
  showConfirmation: () => Promise<RotationResolverType>;
};
const DeleteContext = React.createContext<RotationContextType>({
  showConfirmation: () => new Promise(() => {}),
});

const { Provider, Consumer } = DeleteContext;

interface OwnProps {
  children: React.ReactNode | React.ReactNode[];
  ConfirmationDialog: React.ComponentType<ConfirmDialogProps>;
}

function RotateKeysEntityContext({ children, ConfirmationDialog }: OwnProps) {
  const [showConfirmationDialog, setShowConfirmationDialog] =
    React.useState(false);
  const resolver = React.useRef<(value: RotationResolverType) => void>();

  function handleShow(): Promise<RotationResolverType> {
    setShowConfirmationDialog(true);

    return new Promise((resolve) => {
      resolver.current = resolve;
    });
  }

  function handleOk() {
    resolver.current?.({ isOk: true });
    setShowConfirmationDialog(false);
  }

  function handleCancel() {
    resolver.current?.({ isOk: false });
    setShowConfirmationDialog(false);
  }

  return (
    <Provider value={{ showConfirmation: handleShow }}>
      {children}
      <ConfirmationDialog
        show={showConfirmationDialog}
        onDelete={handleOk}
        onCancel={handleCancel}
      />
    </Provider>
  );
}

const useRotateKeysDialog = () => React.useContext(DeleteContext);

export {
  RotateKeysEntityContext,
  Consumer as RotateEntityContextConsumer,
  useRotateKeysDialog,
};
