// @flow strict
import * as React from 'react';

type Props = {
  children: React.Node,
};

const stopPropagation = (evt) => {
  evt.stopPropagation();
  evt.preventDefault();
};

const StopPropagation = ({ children }: Props) => (
  <span role="presentation" onClick={stopPropagation} onMouseDown={stopPropagation}>
    {children}
  </span>
);

export default StopPropagation;
