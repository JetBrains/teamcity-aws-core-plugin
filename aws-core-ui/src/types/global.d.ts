export {};

declare global {
  function renderEditAwsConnection(config: ConfigWrapper): void;

  interface Window {
    BS: {
      OAuthConnectionDialog: {
        recenterDialog(): void;
      };
      helpUrlPrefix?: string;
      Encrypt: {
        encryptData: (value: string, publicKey: string) => string;
      };
      Util: {
        showHelp: (
          event: React.MouseEvent<HTMLAnchorElement, MouseEvent>,
          href: string,
          { width: number, height: number }
        ) => void;
      };
    };
    $j: JQueryStatic;
  }
}
