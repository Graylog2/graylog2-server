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

import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';
import NavigationLink from 'components/navigation/NavigationLink';
import useLocation from 'routing/useLocation';
import isActiveRoute from 'components/navigation/util/isActiveRoute';
import { NavDropdown } from 'components/bootstrap';
import AppConfig from 'util/AppConfig';

const requiredFeatureFlagIsEnabled = (requiredFeatureFlag: undefined | string) => (requiredFeatureFlag ? AppConfig.isFeatureEnabled(requiredFeatureFlag) : true);

type PluginNavLinkProps = {
  path: string;
  description: string;
  BadgeComponent?: React.ComponentType<{ text: string }>
  topLevel?: boolean
}

const PluginNavLink = ({
  description,
  path,
  BadgeComponent = undefined,
  topLevel = false,
}: PluginNavLinkProps) => (
  <NavigationLink key={description}
                  description={BadgeComponent ? <BadgeComponent text={description} /> : description}
                  path={path}
                  topLevel={topLevel} />
);

type PluginNavDropdownProps = {
  navigationItem: PluginNavigation,
}

const PluginNavDropdown = ({
  navigationItem: {
    children,
    description,
    BadgeComponent,
  },
}: PluginNavDropdownProps) => {
  const { pathname } = useLocation();
  const currentUser = useCurrentUser();

  const activeChild = children.filter(({ path, end }) => (path && isActiveRoute(pathname, path, end)));
  const title = activeChild.length > 0 ? `${description} / ${activeChild[0].description}` : description;
  const isEmpty = !children.some((child) => (
    isPermitted(currentUser.permissions, child.permissions) && requiredFeatureFlagIsEnabled(child.requiredFeatureFlag)),
  );

  if (isEmpty) {
    return null;
  }

  const renderBadge = children.some((child) => isPermitted(currentUser.permissions, child.permissions) && child?.BadgeComponent);

  return (
    <NavDropdown key={title}
                 title={title}
                 badge={renderBadge ? BadgeComponent : null}
                 inactiveTitle={description}>
      {children.map((childNavigationItem) => (
        <PluginNavLink description={childNavigationItem.description}
                       path={childNavigationItem.path}
                       key={childNavigationItem.description} />
      ))}
    </NavDropdown>
  );
};

const NavigationItem = ({ navigationItem }: { navigationItem: PluginNavigation }) => {
  const currentUser = useCurrentUser();

  if (!requiredFeatureFlagIsEnabled(navigationItem.requiredFeatureFlag)) {
    return null;
  }

  if (navigationItem.permissions && !isPermitted(currentUser.permissions, navigationItem.permissions)) return null;

  if (navigationItem.useCondition) {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    const shouldBeVisible = navigationItem.useCondition();

    if (!shouldBeVisible) {
      return false;
    }
  }

  if (navigationItem.children) {
    return (
      <PluginNavDropdown navigationItem={navigationItem}
                         key={navigationItem.description} />
    );
  }

  return (
    <PluginNavLink path={navigationItem.description}
                   description={navigationItem.description}
                   key={navigationItem.description}
                   topLevel />
  );
};

export default NavigationItem;
