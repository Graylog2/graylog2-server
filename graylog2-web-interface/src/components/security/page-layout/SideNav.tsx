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

import { Icon } from 'components/common';

export const NAV_BAR_WIDTH = 200;

function leftPosition(isOpen: boolean) {
  if (isOpen) {
    return '20px';
  }

  return `${NAV_BAR_WIDTH + 10}px`;
}

const NavToggleButton = styled.button<{ $navIsOpen: boolean }>(({ $navIsOpen }) => css`
  position: absolute;
  left: ${leftPosition($navIsOpen)};
  bottom: 20px;
  z-index: 11;
  transition: all 0.33s ease-in-out;
  border-radius: 50%;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  height: 3rem;
  width: 3rem;
  border: 0;
  background-color: transparent;
  cursor: pointer;
  outline: none;
  color: ${({ theme }) => theme.colors.gray[60]};
  font-size: ${({ theme }) => theme.fonts.size.extraLarge};
  padding: 0.5rem;

  &:hover {
    background-color: ${({ theme }) => theme.colors.gray[80]};
  }

  &:active {
    background-color: ${({ theme }) => theme.colors.gray[70]};
  }
`);

const Nav = styled.div<{ $isOpen: boolean }>(({ $isOpen, theme }) => css`
  background-color: ${theme.colors.global.background};
  border-right: 1px solid ${theme.colors.global.contentBackground};
  height: 100%;
  width: ${NAV_BAR_WIDTH}px;
  transition: all 0.33s ease-in-out;
  position: absolute;
  left: ${$isOpen ? '0px' : `-${NAV_BAR_WIDTH}px`};
  z-index: 11;

  ul {
    list-style: none;
    margin: 0;
    padding: 0;
  }
`);

type Props = React.PropsWithChildren<{
  isOpen: boolean,
  toggleIsOpen: () => void,
}>

const SideNav = ({ children, isOpen, toggleIsOpen }: Props) => (
  <Nav $isOpen={isOpen}>
    <ul>
      {children}
    </ul>
    <NavToggleButton $navIsOpen={isOpen} onClick={toggleIsOpen}>
      <Icon name={isOpen ? 'arrow-left' : 'th'} />
    </NavToggleButton>
  </Nav>
);

export default SideNav;
