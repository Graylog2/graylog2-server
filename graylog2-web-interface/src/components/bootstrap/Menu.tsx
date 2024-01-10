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
  onChange?: (isOpen: boolean) => void,
  opened?: boolean,
  keepMounted?: boolean,
  position?: MenuProps['position'],
  shadow?: MenuProps['shadow'],
  width?: number,
  withinPortal?: boolean,
}>

const Menu = ({ children, shadow, width, withinPortal, position, opened, onChange, keepMounted }: Props) => {
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
    <MantineMenu shadow={shadow}
                 opened={opened}
                 onChange={onChange}
                 width={width}
                 position={position}
                 withinPortal={withinPortal}
                 styles={styles}
                 keepMounted={keepMounted}>
      {children}
    </MantineMenu>
  );
};

const StyledMenuItem = styled(MantineMenu.Item)(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
  font-size: ${theme.fonts.size.body};

  a {
    color: ${theme.colors.global.textDefault};
    text-decoration: none;
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
  position: undefined,
  shadow: undefined,
  width: undefined,
  withinPortal: false,
  opened: undefined,
  onChange: undefined,
  keepMounted: undefined,
};

export default Menu;
