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
import { useMemo } from 'react';
import type { PluginNavigation } from 'graylog-web-plugin';

import { Nav } from 'components/bootstrap';
import { isPermitted } from 'util/PermissionsMixin';
import Routes, { ENTERPRISE_ROUTE_DESCRIPTION, SECURITY_ROUTE_DESCRIPTION } from 'routing/Routes';
import filterByPerspective from 'components/perspectives/util/filterByPerspective';
import useCurrentUser from 'hooks/useCurrentUser';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';
import usePluginEntities from 'hooks/usePluginEntities';
import { navigation as securityNavigation } from 'components/security/bindings';
import NavigationItem from 'components/navigation/NavigationItem';

const LAST_POSITION = 'last';

const _existingDropdownItemIndex = (
  existingNavigationItems: Array<PluginNavigation>,
  newNavigationItem: PluginNavigation,
) => {
  if (!newNavigationItem.children) {
    return -1;
  }

  return existingNavigationItems.findIndex(
    ({ description, perspective, children }) =>
      newNavigationItem.description === description && newNavigationItem.perspective === perspective && children,
  );
};

const mergeDuplicateDropdowns = (navigationItems: Array<PluginNavigation>): Array<PluginNavigation> =>
  navigationItems.reduce((result, current) => {
    const existingDropdownItemIndex = _existingDropdownItemIndex(result, current);

    if (existingDropdownItemIndex >= 0) {
      const existingDropdownItem = result[existingDropdownItemIndex];
      const newDropdownItem = {
        ...current,
        ...existingDropdownItem,
        children: [...existingDropdownItem.children, ...current.children],
      };
      const newResult = [...result];
      newResult[existingDropdownItemIndex] = newDropdownItem;

      return newResult;
    }

    return [...result, current];
  }, []);

const pluginMenuItemExists = (navigationItems: Array<PluginNavigation>, description: string) => {
  if (!navigationItems?.length) {
    return false;
  }

  return !!navigationItems.find((value) => value.description?.toLowerCase() === description.toLowerCase());
};

const pluginLicenseValid = (navigationItems: Array<PluginNavigation>, description: string) => {
  if (!navigationItems?.length) return false;
  const menuItem = navigationItems.find((value) => value.description?.toLowerCase() === description.toLowerCase());

  return menuItem && Object.keys(menuItem).includes('useIsValidLicense') ? menuItem.useIsValidLicense() : true;
};

const sortItemsByPosition = <T extends { position: typeof LAST_POSITION | undefined }>(navigationItems: Array<T>) =>
  navigationItems.sort((route1, route2) => {
    if (route1.position === LAST_POSITION) {
      return 1;
    }

    if (route2.position === LAST_POSITION) {
      return -1;
    }

    return 0;
  });

const useNavigationItems = () => {
  const { permissions } = useCurrentUser();
  const { activePerspective } = useActivePerspective();
  const allNavigationItems = usePluginEntities('navigation');

  return useMemo(() => {
    const navigationItems = mergeDuplicateDropdowns(allNavigationItems);
    const enterpriseMenuIsMissing = !pluginMenuItemExists(navigationItems, ENTERPRISE_ROUTE_DESCRIPTION);
    const securityMenuIsMissing = !pluginMenuItemExists(navigationItems, SECURITY_ROUTE_DESCRIPTION);
    const securityLicenseInvalid = !pluginLicenseValid(navigationItems, SECURITY_ROUTE_DESCRIPTION);
    const isPermittedToEnterpriseOrSecurity = isPermitted(permissions, ['licenseinfos:read']);

    if (enterpriseMenuIsMissing && isPermittedToEnterpriseOrSecurity) {
      // no enterprise plugin menu, so we will add one
      navigationItems.push({
        path: Routes.SYSTEM.ENTERPRISE,
        description: ENTERPRISE_ROUTE_DESCRIPTION,
      });
    }

    if ((securityMenuIsMissing && isPermittedToEnterpriseOrSecurity) || securityLicenseInvalid) {
      // no security plugin menu, so we will add one
      if (!securityMenuIsMissing) {
        // remove the existing security menu item
        navigationItems.splice(
          navigationItems.findIndex((item) => item.description === SECURITY_ROUTE_DESCRIPTION),
          1,
        );
      }

      navigationItems.push(securityNavigation);
    }

    const itemsForActivePerspective = filterByPerspective(navigationItems, activePerspective?.id);

    return sortItemsByPosition(itemsForActivePerspective);
  }, [activePerspective, allNavigationItems, permissions]);
};

const MainNavbar = ({ pathname }: { pathname: string }) => {
  const navigationItems = useNavigationItems();

  return (
    <Nav className="navbar-main">
      {navigationItems.map((navigationItem) => (
        <NavigationItem navigationItem={navigationItem} pathname={pathname} key={navigationItem.description} />
      ))}
    </Nav>
  );
};

export default MainNavbar;
