import React, { useContext } from 'react';
import styled from 'styled-components';

import { Button } from 'components/graylog';
import { Icon } from 'components/common';
import { ScratchpadContext } from 'providers/ScratchpadProvider';

const Toggle = styled(Button)`
  padding-left: 6px;
  padding-right: 6px;
  background: none;
  border: 0;
`;

const ScratchpadToggle = () => {
  const { toggleScratchpadVisibility } = useContext(ScratchpadContext);

  return (
    <li role="presentation">
      <Toggle bsStyle="link"
              type="button"
              aria-label="Scratchpad"
              id="scratchpad-toggle"
              onClick={toggleScratchpadVisibility}>
        <Icon name="edit" size="lg" fixedWidth title="Scratchpad" />
      </Toggle>
    </li>

  );
};

export default ScratchpadToggle;
