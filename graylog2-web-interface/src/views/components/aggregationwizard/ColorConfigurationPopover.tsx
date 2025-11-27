import styled, { css } from 'styled-components';
import React, { useState, useCallback } from 'react';

import Popover from 'components/common/Popover';
import { ColorPicker } from 'components/common';
import { colors as defaultColors } from 'views/components/visualizations/Colors';

export const ColorHintWrapper = styled.div`
  width: 25px;
  height: 25px;
  display: flex;
  justify-content: center;
  align-items: center;
`;

export const ColorHint = styled.div(
  ({ color, theme }) => css`
    cursor: pointer;
    background-color: ${color};
    width: ${theme.spacings.md};
    height: ${theme.spacings.md};
  `,
);

const ColorConfigurationPopover = ({ onColorSelect, curColor, title }) => {
  const [showPopover, setShowPopover] = useState(false);
  const togglePopover = useCallback(() => setShowPopover((show) => !show), []);
  const _onColorSelect = useCallback(
    (color: string) => {
      onColorSelect(color);
      togglePopover();
    },
    [onColorSelect, togglePopover],
  );

  return (
    <Popover position="top" withArrow opened={showPopover}>
      <Popover.Target>
        <ColorHintWrapper>
          <ColorHint aria-label="Color Hint" onClick={togglePopover} color={curColor} />
        </ColorHintWrapper>
      </Popover.Target>
      <Popover.Dropdown title={title}>
        <ColorPicker color={curColor} colors={defaultColors} onChange={_onColorSelect} />
      </Popover.Dropdown>
    </Popover>
  );
};

export default ColorConfigurationPopover;
