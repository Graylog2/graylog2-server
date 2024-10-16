import React from 'react';

import { Icon } from 'components/common';

type Props = {
  builtIn: boolean,
};

const BuiltInCell = ({ builtIn }: Props) => {
  if (!builtIn) return null;

  return (<Icon name="check_circle" />);
};

export default BuiltInCell;
