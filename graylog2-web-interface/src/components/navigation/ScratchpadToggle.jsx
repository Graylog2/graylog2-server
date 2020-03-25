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
              onClick={toggleScratchpadVisibility}>
        <Icon name="edit" size="lg" fixedWidth aria-label="Scratchpad" title="Scratchpad" />
      </Toggle>
    </li>

  );
};

export default ScratchpadToggle;
