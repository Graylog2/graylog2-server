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
import * as React from 'react';
import styled, { css } from 'styled-components';

import NavItemStateIndicator from 'components/common/NavItemStateIndicator';
import { NAVBAR_GAP } from 'theme/constants';

const NavigationLink = styled.a<{ $hasOnClick: boolean }>(
  ({ theme, $hasOnClick }) => css`
    color: ${theme.colors.text.primary};

    ${$hasOnClick &&
    css`
      padding-left: ${NAVBAR_GAP}px;
      padding-right: ${NAVBAR_GAP}px;
    `}

    &:hover,
    &:focus {
      color: ${theme.colors.variant.darker.default};
      background-color: transparent;

      /* Bootstrap's base stylesheet underlines anchors on hover, which its own nav rules used to
         suppress. Those no longer apply here, so navigation items suppress it themselves. */
      text-decoration: none;
    }

    /* Bootstrap also rings a focused anchor whichever way it was focused, which browsers otherwise
       avoid doing after a click, so clicking a navigation item would leave its ring behind. Reaching
       one by keyboard still shows it. */
    &:focus:not(:focus-visible) {
      outline: none;
    }
  `,
);
const NavItem = ({ children = undefined, ...props }: React.ComponentProps<typeof NavItem>) => (
  <NavigationLink {...props} $hasOnClick={Boolean(props.onClick)}>
    <NavItemStateIndicator>{children}</NavItemStateIndicator>
  </NavigationLink>
);

NavItem.displayName = 'NavItem';

/** @component */
export default NavItem;
