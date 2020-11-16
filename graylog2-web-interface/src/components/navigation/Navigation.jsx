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
// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';
import naturalSort from 'javascript-natural-sort';
import { PluginStore } from 'graylog-web-plugin/plugin';

import withLocation from 'routing/withLocation';
import { LinkContainer } from 'components/graylog/router';
import { appPrefixed } from 'util/URLUtils';
import { IfPermitted } from 'components/common';
import { isPermitted } from 'util/PermissionsMixin';
import { Navbar, Nav, NavItem, NavDropdown } from 'components/graylog';
import CurrentUserContext from 'contexts/CurrentUserContext';
import GlobalThroughput from 'components/throughput/GlobalThroughput';
import Routes from 'routing/Routes';

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

const formatSinglePluginRoute = ({ description, path, permissions }, topLevel = false) => {
  const link = <NavigationLink key={description} description={description} path={appPrefixed(path)} topLevel={topLevel} />;

  if (permissions) {
    return <IfPermitted key={description} permissions={permissions}>{link}</IfPermitted>;
  }

  return link;
};

const formatPluginRoute = (pluginRoute, permissions, location) => {
  if (pluginRoute.children) {
    const activeChild = pluginRoute.children.filter(({ path }) => (path && _isActive(location.pathname, path)));
    const title = activeChild.length > 0 ? `${pluginRoute.description} / ${activeChild[0].description}` : pluginRoute.description;
    const isEmpty = !pluginRoute.children.some((child) => isPermitted(permissions, child.permissions));

    if (isEmpty) {
      return null;
    }

    return (
      <NavDropdown key={title} title={title} id="enterprise-dropdown">
        {pluginRoute.children.map((child) => formatSinglePluginRoute(child, false))}
      </NavDropdown>
    );
  }

  return formatSinglePluginRoute(pluginRoute, true);
};

type Props = {
  location: {
    pathname: string,
  },
};

const Navigation = ({ location }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const { permissions, full_name: fullName, read_only: readOnly, id: userId } = currentUser || {};

  const pluginExports = PluginStore.exports('navigation');

  const enterpriseMenuIsMissing = !pluginExports.find((value) => value.description.toLowerCase() === 'enterprise');
  const isPermittedToEnterprise = isPermitted(permissions, ['licenseinfos:read']);

  if (enterpriseMenuIsMissing && isPermittedToEnterprise) {
    // no enterprise plugin menu, so we will add one
    pluginExports.push({
      path: Routes.SYSTEM.ENTERPRISE,
      description: 'Enterprise',
    });
  }

  const pluginNavigations = pluginExports
    .sort((route1, route2) => naturalSort(route1.description.toLowerCase(), route2.description.toLowerCase()))
    .map((pluginRoute) => formatPluginRoute(pluginRoute, permissions, location));
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
        <Nav navbar>
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
          <LinkContainer to={Routes.SYSTEM.NODES.LIST}>
            <GlobalThroughput />
          </LinkContainer>

          <InactiveNavItem className="dev-badge-wrap">
            <DevelopmentHeaderBadge />
            {pluginItems.map(({ key, component: Item }) => <Item key={key} />)}
          </InactiveNavItem>

          <ScratchpadToggle />

          <HelpMenu active={_isActive(location.pathname, Routes.GETTING_STARTED)} />

          <UserMenu fullName={fullName} readOnly={readOnly} userId={userId} />
        </Nav>
      </Navbar.Collapse>
    </StyledNavbar>
  );
};

Navigation.propTypes = {
  location: PropTypes.shape({
    pathname: PropTypes.string.isRequired,
  }).isRequired,
};

export default withLocation(Navigation);
