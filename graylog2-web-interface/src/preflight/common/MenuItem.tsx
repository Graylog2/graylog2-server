import React from 'react';
import type { MenuItemProps } from '@mantine/core';
import { Menu } from '@mantine/core';
import { Link } from 'react-router-dom';

type MenuItemButtonProps = typeof Menu.Item;

type Props = {
  href?: string,
  onSelect?: () => void,
  icon?: MenuItemProps['icon'],
  label: React.ReactNode,
  key?: React.Key,
};

const MenuItem = ({ children, ...otherProps }: MenuItemButtonProps) => {
  return (
    <Menu.Item {...otherProps}>
      {children}
    </Menu.Item>
  );
};

// It looks like it is not possible to wrap a `Menu.Item`, since `Menu` is filtering its children.
// At the moment we can work around this by using this function to get a `Menu.Item`.
// eslint-disable-next-line import/prefer-default-export
export const menuItemAdapter = ({ key, href, icon, onSelect, label }: Props) => {
  if (href) {
    return (
      <Menu.Item key={key} icon={icon} component={Link} to={href}>
        {label}
      </Menu.Item>
    );
  }

  return (
    <Menu.Item icon={icon} onClick={onSelect}>
      {label}
    </Menu.Item>
  );
};

export default MenuItem as typeof Menu.Item;
