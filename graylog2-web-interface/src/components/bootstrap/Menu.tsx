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
import type { PropsWithChildren } from 'react';
import React from 'react';
import { Menu as MantineMenu, type MenuProps } from '@mantine/core';
import styled, { css, useTheme } from 'styled-components';

type Props = PropsWithChildren<{
  closeOnItemClick?: boolean,
  onChange?: (isOpen: boolean) => void,
  onClose?: () => void,
  opened?: boolean,
  portalProps?: { target: HTMLElement },
  keepMounted?: boolean,
  position?: MenuProps['position'],
  shadow?: MenuProps['shadow'],
  width?: number,
  withinPortal?: boolean,
  zIndex?: number,
}>

const Menu = ({
  children,
  closeOnItemClick,
  onClose,
  shadow,
  width,
  withinPortal,
  position,
  opened,
  onChange,
  portalProps,
  keepMounted,
  zIndex,
}: Props) => {
  const theme = useTheme();

  const styles = () => ({
    dropdown: {
      backgroundColor: theme.colors.global.contentBackground,
      border: `1px solid ${theme.colors.variant.lighter.default}`,
      fontFamily: theme.fonts.family.navigation,
      fontSize: theme.fonts.size.navigation,
    },
  });

  return (
    <MantineMenu closeOnItemClick={closeOnItemClick}
                 onClose={onClose}
                 shadow={shadow}
                 opened={opened}
                 onChange={onChange}
                 portalProps={portalProps}
                 width={width}
                 position={position}
                 withinPortal={withinPortal}
                 styles={styles}
                 keepMounted={keepMounted}
                 zIndex={zIndex}>
      {children}
    </MantineMenu>
  );
};

const StyledMenuItem = styled(MantineMenu.Item)(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
  font-size: ${theme.fonts.size.body};
  white-space: nowrap;
  
  &:hover, &:focus {
    text-decoration: none;  
    color: inherit;
  }
`);

const StyledMenuDivider = styled(MantineMenu.Divider)(({ theme }) => css`
  border-color: ${theme.colors.variant.lighter.default};
`);

const StyledMenuLabel = styled(MantineMenu.Label)(({ theme }) => css`
  font-size: ${theme.fonts.size.navigation};
`);

Menu.Target = MantineMenu.Target;
Menu.Dropdown = MantineMenu.Dropdown;
Menu.Item = StyledMenuItem;
Menu.Divider = StyledMenuDivider;
Menu.Label = StyledMenuLabel;

Menu.defaultProps = {
  closeOnItemClick: true,
  position: undefined,
  portalProps: undefined,
  shadow: undefined,
  width: undefined,
  withinPortal: false,
  opened: undefined,
  onChange: undefined,
  onClose: undefined,
  keepMounted: undefined,
  zIndex: undefined,
};

export default Menu;
