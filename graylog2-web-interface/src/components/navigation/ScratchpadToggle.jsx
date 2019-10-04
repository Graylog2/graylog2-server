import React, { useContext } from 'react';

import { Button } from 'components/graylog';

import { ScratchpadContext } from '../../routing/context/ScratchpadProvider';

const ScratchpadToggle = () => {
  const { isScratchpadVisible, setScratchpadVisibility } = useContext(ScratchpadContext);

  return (
    <Button bsStyle="link" type="button" onClick={() => setScratchpadVisibility(!isScratchpadVisible)}>
      <i className="fa fa-edit fa-lg fa-fw" aria-label="Scratchpad" title="Scratchpad" />
    </Button>
  );
};

export default ScratchpadToggle;
