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
import PropTypes from 'prop-types';
import { Menu as MantineMenu, type MenuProps } from '@mantine/core';
import { useTheme } from 'styled-components';

const Menu = ({ children, ...otherProps }: MenuProps) => {
  const theme = useTheme();

  const menuStyles = () => ({
    dropdown: {
      backgroundColor: theme.colors.global.contentBackground,
      border: `1px solid ${theme.colors.variant.lighter.default}`,
    },
  });

  return (
    <MantineMenu {...otherProps} styles={menuStyles}>
      {children}
    </MantineMenu>
  );
};

Menu.Target = MantineMenu.Target;
Menu.Dropdown = MantineMenu.Dropdown;
Menu.Item = MantineMenu.Item;

Menu.propTypes = {
  children: PropTypes.node.isRequired,
};

export default Menu;
