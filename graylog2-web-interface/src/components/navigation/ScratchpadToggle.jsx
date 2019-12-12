import React, { useContext } from 'react';
import styled from 'styled-components';

import { Button } from 'components/graylog';
import { ScratchpadContext } from 'providers/ScratchpadProvider';

const Toggle = styled(Button)`
  padding-left: 6px;
  padding-right: 6px;
`;

const ScratchpadToggle = () => {
  const { toggleScratchpadVisibility } = useContext(ScratchpadContext);

  return (
    <li role="presentation">
      <Toggle bsStyle="link" type="button" onClick={toggleScratchpadVisibility}>
        <i className="fa fa-edit fa-lg fa-fw" aria-label="Scratchpad" title="Scratchpad" />
      </Toggle>
    </li>

  );
};

export default ScratchpadToggle;
