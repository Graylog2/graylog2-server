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
import type * as Immutable from 'immutable';

import useCurrentUser from 'hooks/useCurrentUser';
import isActiveRoute from 'components/navigation/util/isActiveRoute';
import { isPermitted } from 'util/PermissionsMixin';
import { NavDropdown } from 'components/bootstrap';
import NavigationLink from 'components/navigation/NavigationLink';
import AppConfig from 'util/AppConfig';

const shouldRender = (
  requiredFeatureFlag: string | undefined,
  requiredPermissions: string | Array<string> | undefined,
  userPermissions: Immutable.List<string>,
) => {
  if (requiredFeatureFlag) {
    return AppConfig.isFeatureEnabled(requiredFeatureFlag);
  }

  if (requiredPermissions) {
    return isPermitted(userPermissions, requiredPermissions);
  }

  return true;
};

const renderLinkTitle = (description: string, Badge: PluginNavigation['BadgeComponent'] | undefined) =>
  Badge ? <Badge text={description} /> : description;

type PluginNavDropdownProps = {
  description: PluginNavigation['description'];
  BadgeComponent: PluginNavigation['BadgeComponent'];
  menuItems: PluginNavigation['children'];
  pathname: string;
};

const PluginNavDropdown = ({ menuItems, description, BadgeComponent, pathname }: PluginNavDropdownProps) => {
  const currentUser = useCurrentUser();
  const activeMenuItem = menuItems.filter(({ path, end }) => path && isActiveRoute(pathname, path, end));
  const title = activeMenuItem.length > 0 ? `${description} / ${activeMenuItem[0].description}` : description;
  const accessibleMenuItems = menuItems.filter(({ requiredFeatureFlag, permissions }) =>
    shouldRender(requiredFeatureFlag, permissions, currentUser.permissions),
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

const NavigationItem = ({
  navigationItem: { requiredFeatureFlag, permissions, children, BadgeComponent, description, path },
  pathname,
}: Props) => {
  const currentUser = useCurrentUser();

  if (!shouldRender(requiredFeatureFlag, permissions, currentUser.permissions)) {
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
    <NavigationLink key={description} description={renderLinkTitle(description, BadgeComponent)} path={path} topLevel />
  );
};

export default NavigationItem;
