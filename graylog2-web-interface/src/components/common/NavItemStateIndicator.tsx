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
import React from 'react';
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';

const indicatorClassName = 'nav-item-state-indicator';
const indicatorPseudoElement = '::before';

// The indicator draws itself along the bottom of this element, so an item whose content is taller
// than a line of text has to let it wrap that content to be drawn in the right place.
export const itemStateIndicatorContainerSelector = `.${indicatorClassName}`;

export const itemStateIndicatorSelector = `${itemStateIndicatorContainerSelector}${indicatorPseudoElement}`;

export const hoverIndicatorStyles = (theme: DefaultTheme) => css`
  ${itemStateIndicatorSelector} {
    border-color: ${theme.colors.gray[70]};
  }
`;

export const activeIndicatorStyles = (theme: DefaultTheme) => css`
  ${itemStateIndicatorSelector} {
    border-color: ${theme.colors.gray[50]};
  }
`;

const Container = styled.div`
  display: inline;
  position: relative;

  &${indicatorPseudoElement} {
    content: ' ';
    position: absolute;
    border-bottom: 1px solid transparent;
    left: 0;
    width: 100%;
    bottom: -4px;
  }
`;

/**
 * This component provides styling for navigation item states like active and hover.
 */
const NavItemStateIndicator = ({ children }: { children: React.ReactNode }) => (
  <Container className={indicatorClassName}>{children}</Container>
);

export default NavItemStateIndicator;
