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
import type { PluginNavigation } from 'graylog-web-plugin';
import styled, { css } from 'styled-components';

import usePermissions from 'hooks/usePermissions';
import isActiveRoute from 'components/navigation/util/isActiveRoute';
import { NavDropdown } from 'components/bootstrap';
import NavigationLink from 'components/navigation/NavigationLink';
import shouldRenderNavigationItem from 'components/navigation/util/shouldRenderNavigationItem';
import { hoverIndicatorStyles, activeIndicatorStyles } from 'components/common/NavItemStateIndicator';

const renderLinkTitle = (description: string, Badge: PluginNavigation['BadgeComponent'] | undefined) =>
  Badge ? <Badge text={description} /> : description;

type PluginNavDropdownProps = {
  description: PluginNavigation['description'];
  BadgeComponent: PluginNavigation['BadgeComponent'];
  menuItems: PluginNavigation['children'];
  pathname: string;
};

const PluginNavDropdown = ({ menuItems, description, BadgeComponent, pathname }: PluginNavDropdownProps) => {
  const { isPermitted } = usePermissions();
  const activeMenuItem = menuItems.filter(({ path, end }) => path && isActiveRoute(pathname, path, end));
  const title = activeMenuItem.length > 0 ? `${description} / ${activeMenuItem[0].description}` : description;
  const accessibleMenuItems = menuItems.filter(
    ({ requiredFeatureFlag, permissions, useCondition }) =>
      // eslint-disable-next-line react-hooks/rules-of-hooks
      (useCondition?.() ?? true) && shouldRenderNavigationItem(requiredFeatureFlag, permissions, isPermitted),
  );

  if (!accessibleMenuItems.length) {
    return null;
  }

  const renderBadge = menuItems.some((menuItem) => menuItem?.BadgeComponent);

  return (
    <NavDropdown key={title} title={title} Badge={renderBadge ? BadgeComponent : undefined} inactiveTitle={description}>
      {accessibleMenuItems.map((menuItem) => (
        <NavigationLink
          key={menuItem.description}
          Badge={menuItem.BadgeComponent}
          description={menuItem.description}
          path={menuItem.path}
        />
      ))}
    </NavDropdown>
  );
};

type Props = {
  pathname: string;
  navigationItem: PluginNavigation;
};

const NavListItem = styled.li(
  ({ theme }) => css`
    > a {
      &:hover,
      &:focus-visible {
        ${hoverIndicatorStyles(theme)}
      }

      &.active {
        ${activeIndicatorStyles(theme)}
      }
    }
  `,
);

const NavigationItem = ({
  navigationItem: { requiredFeatureFlag, permissions, children, BadgeComponent, description, path },
  pathname,
}: Props) => {
  const { isPermitted } = usePermissions();

  if (!shouldRenderNavigationItem(requiredFeatureFlag, permissions, isPermitted)) {
    return null;
  }

  if (children) {
    return (
      <PluginNavDropdown
        menuItems={children}
        description={description}
        BadgeComponent={BadgeComponent}
        pathname={pathname}
        key={description}
      />
    );
  }

  return (
    <NavListItem>
      <NavigationLink
        key={description}
        description={renderLinkTitle(description, BadgeComponent)}
        path={path}
        topLevel
      />
    </NavListItem>
  );
};

export default NavigationItem;
