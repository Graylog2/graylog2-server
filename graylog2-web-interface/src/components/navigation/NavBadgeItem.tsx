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

import { LinkContainer } from 'components/common';
import { Nav } from 'components/bootstrap';
import { NAV_ITEM_HEIGHT } from 'theme/constants';

import InactiveNavItem from './InactiveNavItem';

const StyledNav = styled(Nav)(
  ({ theme }) => css`
    --nav-item-horizontal-padding: ${theme.spacings.sm};

    > li > a {
      min-height: ${NAV_ITEM_HEIGHT};
      display: inline-flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;

      * {
        cursor: inherit;
      }
    }
  `,
);

const StyledInactiveNavItem = styled(InactiveNavItem)`
  a:hover {
    border: 0;
    text-decoration: none;
  }
`;

type Props = React.PropsWithChildren<{
  onClick?: () => void;
  to: string;
}>;

const NavBadgeItem = ({ children = undefined, onClick = undefined, to }: Props) => (
  <StyledNav>
    <li>
      <LinkContainer to={to} onClick={onClick}>
        <StyledInactiveNavItem>{children}</StyledInactiveNavItem>
      </LinkContainer>
    </li>
  </StyledNav>
);

export default NavBadgeItem;
