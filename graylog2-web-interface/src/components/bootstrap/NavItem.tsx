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
import { NAVBAR_GAP, NAV_ITEM_HEIGHT } from 'theme/constants';

const NavigationLink = styled.a<{ $withMinHeight: boolean }>(
  ({ theme, $withMinHeight }) => css`
    color: ${theme.colors.text.primary};
    ${$withMinHeight &&
    css`
      min-height: ${NAV_ITEM_HEIGHT};
    `}
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding-left: ${NAVBAR_GAP}px;
    padding-right: ${NAVBAR_GAP}px;

    &:hover,
    &:focus {
      color: ${theme.colors.variant.darker.default};
      background-color: transparent;

      text-decoration: none;
    }

    &:focus:not(:focus-visible) {
      outline: none;
    }
  `,
);
const NavItem = ({
  children = undefined,
  withMinHeight = true,
  ...props
}: React.ComponentProps<typeof NavItem> & { withMinHeight?: boolean }) => (
  <NavigationLink {...props} $withMinHeight={withMinHeight}>
    <NavItemStateIndicator>{children}</NavItemStateIndicator>
  </NavigationLink>
);

NavItem.displayName = 'NavItem';

/** @component */
export default NavItem;
