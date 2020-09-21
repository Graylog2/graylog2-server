// @flow strict
import * as React from 'react';
import { LinkContainer as BootstrapLinkContainer } from 'react-router-bootstrap';

type LinkContainerProps = {
  to: string,
  children: React.Node,
};
export const LinkContainer = ({ children, to }: LinkContainerProps) => <BootstrapLinkContainer to={to}>{children}</BootstrapLinkContainer>;
