// @flow strict
import * as React from 'react';
import InteractiveContext from '../contexts/InteractiveContext';

type Props = {
  children: React.Node,
};
const IfInteractive = ({ children }: Props) => (
  <InteractiveContext.Consumer>
    {(interactive) => (interactive ? children : null)}
  </InteractiveContext.Consumer>
);

export default IfInteractive;
