import React from 'react';
import { Menu as MantineMenu, type MenuDropdownProps } from '@mantine/core';

const MenuDropdownWrapper = ({ children, ...otherProps }: MenuDropdownProps) => {
  return (
    <MantineMenu.Dropdown {...otherProps}>
      {children}
    </MantineMenu.Dropdown>
  );
};

export default MenuDropdownWrapper;
