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
import { useLocation } from 'react-router-dom';
import { PluginStore } from 'graylog-web-plugin/plugin';
import type { PluginNavigationDropdownItem, PluginNavigation } from 'graylog-web-plugin';
import type * as Immutable from 'immutable';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { LinkContainer } from 'components/common/router';
import { appPrefixed } from 'util/URLUtils';
import AppConfig from 'util/AppConfig';
import { Navbar, Nav, NavItem, NavDropdown } from 'components/bootstrap';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import GlobalThroughput from 'components/throughput/GlobalThroughput';
import Routes, { ENTERPRISE_ROUTE_DESCRIPTION, SECURITY_ROUTE_DESCRIPTION } from 'routing/Routes';

import UserMenu from './UserMenu';
import HelpMenu from './HelpMenu';
import NavigationBrand from './NavigationBrand';
import NotificationBadge from './NotificationBadge';
import NavigationLink from './NavigationLink';
import DevelopmentHeaderBadge from './DevelopmentHeaderBadge';
import SystemMenu from './SystemMenu';
import InactiveNavItem from './InactiveNavItem';
import ScratchpadToggle from './ScratchpadToggle';
import StyledNavbar from './Navigation.styles';

const _isActive = (requestPath, prefix) => {
  return requestPath.indexOf(appPrefixed(prefix)) === 0;
};

/**
 * Checks if a plugin and its corresponding route is registered to the PluginStore
 *
 * @param {string} description
 */
function pluginMenuItemExists(description: string): boolean {
  const pluginExports = PluginStore.exports('navigation');

  if (!pluginExports) return false;

  return !!pluginExports.find((value) => value.description?.toLowerCase() === description.toLowerCase());
}

const formatSinglePluginRoute = ({ description, path, permissions, requiredFeatureFlag }: PluginNavigationDropdownItem, currentUserPermissions: Immutable.List<string>, topLevel = false) => {
  if (permissions && !isPermitted(currentUserPermissions, permissions)) {
    return null;
  }

  if (requiredFeatureFlag && !AppConfig.isFeatureEnabled(requiredFeatureFlag)) {
    return null;
  }

  return <NavigationLink key={description} description={description} path={appPrefixed(path)} topLevel={topLevel} />;
};

const formatPluginRoute = (pluginRoute: PluginNavigation, currentUserPermissions: Immutable.List<string>, pathname: string) => {
  if (pluginRoute.requiredFeatureFlag && !AppConfig.isFeatureEnabled(pluginRoute.requiredFeatureFlag)) {
    return null;
  }

  if (pluginRoute.children) {
    const activeChild = pluginRoute.children.filter(({ path }) => (path && _isActive(pathname, path)));
    const title = activeChild.length > 0 ? `${pluginRoute.description} / ${activeChild[0].description}` : pluginRoute.description;
    const isEmpty = !pluginRoute.children.some((child) => isPermitted(currentUserPermissions, child.permissions) && (child.requiredFeatureFlag ? AppConfig.isFeatureEnabled(child.requiredFeatureFlag) : true));

    if (isEmpty) {
      return null;
    }

    return (
      <NavDropdown key={title} title={title} id="enterprise-dropdown">
        {pluginRoute.children.map((child) => formatSinglePluginRoute(child, currentUserPermissions, false))}
      </NavDropdown>
    );
  }

  return formatSinglePluginRoute(pluginRoute, currentUserPermissions, true);
};

type Props = {
  pathname: string,
};

const Navigation = React.memo(({ pathname }: Props) => {
  const currentUser = useCurrentUser();
  const { permissions, fullName, readOnly, id: userId } = currentUser || {};

  const pluginExports = PluginStore.exports('navigation');

  const enterpriseMenuIsMissing = !pluginMenuItemExists(ENTERPRISE_ROUTE_DESCRIPTION);
  const securityMenuIsMissing = !pluginMenuItemExists(SECURITY_ROUTE_DESCRIPTION);

  const isPermittedToEnterpriseOrSecurity = isPermitted(permissions, ['licenseinfos:read']);

  if (enterpriseMenuIsMissing && isPermittedToEnterpriseOrSecurity) {
    // no enterprise plugin menu, so we will add one
    pluginExports.push({
      path: Routes.SYSTEM.ENTERPRISE,
      description: ENTERPRISE_ROUTE_DESCRIPTION,
    });
  }

  if (securityMenuIsMissing && isPermittedToEnterpriseOrSecurity) {
    // no security plugin menu, so we will add one
    pluginExports.push({
      path: Routes.SECURITY,
      description: SECURITY_ROUTE_DESCRIPTION,
    });
  }

  const pluginNavigations = pluginExports
    .sort((route1, route2) => naturalSort(route1.description.toLowerCase(), route2.description.toLowerCase()))
    .map((pluginRoute) => formatPluginRoute(pluginRoute, currentUser.permissions, pathname));
  const pluginItems = PluginStore.exports('navigationItems');

  return (
    <StyledNavbar fluid fixedTop>
      <Navbar.Header>
        <Navbar.Brand>
          <LinkContainer to={Routes.STARTPAGE}>
            <NavigationBrand />
          </LinkContainer>
        </Navbar.Brand>
        <Navbar.Toggle />
        <DevelopmentHeaderBadge smallScreen />
        {pluginItems.map(({ key, component: Item }) => <Item key={key} smallScreen />)}
      </Navbar.Header>

      <Navbar.Collapse>
        <Nav navbar className="navbar-main">
          <LinkContainer to={Routes.SEARCH}>
            <NavItem to="search">Search</NavItem>
          </LinkContainer>

          <LinkContainer to={Routes.STREAMS}>
            <NavItem>Streams</NavItem>
          </LinkContainer>

          <LinkContainer to={Routes.ALERTS.LIST}>
            <NavItem>Alerts</NavItem>
          </LinkContainer>

          <LinkContainer to={Routes.DASHBOARDS}>
            <NavItem>Dashboards</NavItem>
          </LinkContainer>

          {pluginNavigations}

          <SystemMenu />
        </Nav>

        <NotificationBadge />

        <Nav navbar pullRight className="header-meta-nav">
          {AppConfig.isCloud() ? (
            <GlobalThroughput disabled />
          ) : (
            <LinkContainer to={Routes.SYSTEM.NODES.LIST}>
              <GlobalThroughput />
            </LinkContainer>
          )}

          <InactiveNavItem className="dev-badge-wrap">
            <DevelopmentHeaderBadge />
            {pluginItems.map(({ key, component: Item }) => <Item key={key} />)}
          </InactiveNavItem>

          <ScratchpadToggle />

          <HelpMenu active={_isActive(pathname, Routes.GETTING_STARTED)} />

          <UserMenu fullName={fullName} readOnly={readOnly} userId={userId} />
        </Nav>
      </Navbar.Collapse>
    </StyledNavbar>
  );
});

const NavigationContainer = () => {
  const { pathname } = useLocation();

  return <Navigation pathname={pathname} />;
};

export default NavigationContainer;
