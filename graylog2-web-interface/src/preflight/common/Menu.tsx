import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { Menu as MantineMenu, type MenuProps } from '@mantine/core';
import { ThemeContext } from 'styled-components';

const Menu = ({ children, ...otherProps }: MenuProps) => {
  const theme = useContext(ThemeContext);

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

Menu.propTypes = {
  children: PropTypes.node.isRequired,
};

export default Menu;
