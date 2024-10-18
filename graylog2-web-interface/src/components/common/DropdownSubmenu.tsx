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

import { Dropdown, MenuItem } from 'components/bootstrap';

type Props = {
  children: React.ReactNode,
  left?: boolean
  title?: string
};

/* stylelint-disable-next-line property-no-unknown */
const Caret = styled.span(({ theme }) => css`
  display: block;
  content: ' ';
  float: right;
  width: 0;
  height: 0;
  border-color: transparent;
  border-style: solid;
  border-width: 5px 0 5px 5px;
  border-left-color: ${theme.colors.gray[80]};
  margin-top: 5px;
  margin-right: -10px;
`);

type StyledSubmenuProps = React.PropsWithChildren<{
  $left: boolean,
  as: string,
}>;
const StyledSubmenu: React.ComponentType<StyledSubmenuProps> = styled(Dropdown)<{ $left: boolean }>(({ $left, theme }) => css`
  position: relative;
  display: flex;
  gap: 5px;

  > a {
    color: ${theme.colors.global.textDefault};
  }

  > .dropdown-menu {
    top: 0;
    left: ${$left ? 'auto' : '100%'};
    right: ${$left ? '98%' : 'auto'};
    margin-top: -6px;
    margin-left: ${$left ? '10px' : '-1px'};
    border-radius: ${$left ? '6px 0 6px 6px' : '0 6px 6px 6px'};
    background-color: ${theme.colors.global.contentBackground};
  }

  &:hover > .dropdown-menu {
    display: block;
  }
`);

const DropdownSubmenu = ({ children, left = false, title }: Props) => (
  <MenuItem>
    <StyledSubmenu $left={left} as="div">
      {title} <Caret />

      <Dropdown.Menu>
        {children}
      </Dropdown.Menu>
    </StyledSubmenu>
  </MenuItem>
);

export default DropdownSubmenu;
