// flow-typed signature: 288bf61535247baaec569a9566b1aa60
// flow-typed version: 5e634ef02e/react-portal_v4.1.x/flow_>=v0.53.x

declare module 'react-portal' {
  declare type PortalProps = {
    children: React$Node,
    node?: Element | null,
  };

  declare class Portal extends React$Component<PortalProps> {}

  declare type RenderFunctionParams = {
    openPortal: (event?: SyntheticEvent<>) => void,
    closePortal: () => void,
    portal: (children: React$Node) => React$Element<typeof Portal>,
    isOpen: boolean,
  };

  declare type PortalWithStateProps = {
    children: (params: RenderFunctionParams) => React$Node,
    node?: Element | null,
    defaultOpen?: boolean,
    closeOnEsc?: boolean,
    closeOnOutsideClick?: boolean,
    onOpen?: () => void,
    onClose?: () => void,
  };

  declare class PortalWithState extends React$Component<PortalWithStateProps> {}

  declare module.exports: {
    Portal: typeof Portal,
    PortalWithState: typeof PortalWithState,
  };
}
