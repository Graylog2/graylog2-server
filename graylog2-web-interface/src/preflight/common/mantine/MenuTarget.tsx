import React from 'react';
import { Menu as MantineMenu, type MenuTargetProps } from '@mantine/core';

const MenuTarget = ({ children, ...otherProps }: MenuTargetProps) => {
  return (
    <MantineMenu.Target {...otherProps}>
      {children}
    </MantineMenu.Target>
  );
};

export default MenuTarget;
