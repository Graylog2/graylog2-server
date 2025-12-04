/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */

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
