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
import React, { useContext } from 'react';
import styled, { css } from 'styled-components';

import { ScratchpadContext } from 'contexts/ScratchpadProvider';
import { Button } from 'components/bootstrap';
import { NAV_ITEM_HEIGHT } from 'theme/constants';
import NavIcon from 'components/navigation/NavIcon';

import NavItemStateIndicator, { hoverIndicatorStyles } from '../common/NavItemStateIndicator';

const Toggle = styled(Button)(
  ({ theme }) => css`
    padding: 0 15px;
    background: none;
    border: 0;
    min-height: ${NAV_ITEM_HEIGHT};
    color: ${theme.colors.text.primary};

    /* The shared button hides its overflow, on itself and on the element wrapping its content, which
       would clip the state indicator sitting just below the icon. Doubled, so that it does not
       depend on which of the two stylesheets is loaded last. */
    && {
      overflow: visible;
    }

    .mantine-Button-label {
      overflow: visible;
    }

    &:hover,
    &:focus-visible {
      ${hoverIndicatorStyles(theme)}
      background: transparent;
      color: ${theme.colors.variant.darker.default};
    }
  `,
);

const ScratchpadToggle = () => {
  const { toggleScratchpadVisibility } = useContext(ScratchpadContext);

  return (
    <li role="presentation">
      <Toggle
        bsStyle="link"
        type="button"
        aria-label="Scratchpad"
        id="scratchpad-toggle"
        onClick={toggleScratchpadVisibility}>
        <NavItemStateIndicator>
          <NavIcon type="scratchpad" title="Scratchpad" />
        </NavItemStateIndicator>
      </Toggle>
    </li>
  );
};

export default ScratchpadToggle;
