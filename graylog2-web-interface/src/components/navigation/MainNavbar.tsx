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
import filterByPerspective from 'components/perspectives/util/filterByPerspective';
import useCurrentUser from 'hooks/useCurrentUser';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';
import usePluginEntities from 'hooks/usePluginEntities';
import NavigationItem from 'components/navigation/NavigationItem';
import { DEFAULT_SECURITY_NAV_ITEM } from 'components/security/bindings';
import DEFAULT_ENTERPRISE_NAV_ITEM from 'components/navigation/DefaultEnterpriseNavItem';
import sortNavigationItems from 'components/navigation/util/sortNavigationItems';

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

const useNavigationItems = () => {
  const { permissions } = useCurrentUser();
  const { activePerspective } = useActivePerspective();
  const allNavigationItems = usePluginEntities('navigation');
  const navigationItems = useMemo(() => mergeDuplicateDropdowns(allNavigationItems), [allNavigationItems]);

  const securityLicenseInvalid = !pluginLicenseValid(navigationItems, DEFAULT_SECURITY_NAV_ITEM.description);

  return useMemo(() => {
    const enterpriseMenuIsMissing = !pluginMenuItemExists(navigationItems, DEFAULT_ENTERPRISE_NAV_ITEM.description);
    const securityMenuIsMissing = !pluginMenuItemExists(navigationItems, DEFAULT_SECURITY_NAV_ITEM.description);
    const isPermittedToEnterpriseOrSecurity = isPermitted(permissions, ['licenseinfos:read']);

    if (enterpriseMenuIsMissing && isPermittedToEnterpriseOrSecurity) {
      // no enterprise plugin menu, so we will add one
      navigationItems.push(DEFAULT_ENTERPRISE_NAV_ITEM);
    }

    if ((securityMenuIsMissing && isPermittedToEnterpriseOrSecurity) || securityLicenseInvalid) {
      // no security plugin menu, so we will add one
      if (!securityMenuIsMissing) {
        // remove the existing security menu item
        navigationItems.splice(
          navigationItems.findIndex((item) => item.description === DEFAULT_SECURITY_NAV_ITEM.description),
          1,
        );
      }

      navigationItems.push(DEFAULT_SECURITY_NAV_ITEM);
    }

    const itemsForActivePerspective = filterByPerspective(navigationItems, activePerspective?.id);

    return sortNavigationItems(itemsForActivePerspective);
  }, [activePerspective?.id, navigationItems, permissions, securityLicenseInvalid]);
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
