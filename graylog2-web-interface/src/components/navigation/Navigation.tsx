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

import useLocation from 'routing/useLocation';
import { LinkContainer } from 'components/common/router';
import AppConfig from 'util/AppConfig';
import { Navbar, Nav, NavItem } from 'components/bootstrap';
import GlobalThroughput from 'components/throughput/GlobalThroughput';
import Routes from 'routing/Routes';
import { Icon } from 'components/common';
import PerspectivesSwitcher from 'components/perspectives/PerspectivesSwitcher';
import usePluginEntities from 'hooks/usePluginEntities';
import MainNavbar from 'components/navigation/MainNavbar';

import UserMenu from './UserMenu';
import HelpMenu from './HelpMenu';
import NotificationBadge from './NotificationBadge';
import DevelopmentHeaderBadge from './DevelopmentHeaderBadge';
import InactiveNavItem from './InactiveNavItem';
import ScratchpadToggle from './ScratchpadToggle';
import StyledNavbar from './Navigation.styles';

type Props = {
  pathname: string,
};

const Navigation = React.memo(({ pathname }: Props) => {
  const pluginItems = usePluginEntities('navigationItems');

  return (
    <StyledNavbar fluid fixedTop collapseOnSelect>
      <Navbar.Header>
        <Navbar.Brand>
          <PerspectivesSwitcher />
        </Navbar.Brand>
        <Navbar.Toggle />
        <DevelopmentHeaderBadge smallScreen />
        {pluginItems.map(({ key, component: Item }) => <Item key={key} smallScreen />)}
      </Navbar.Header>
      <Navbar.Collapse>
        <MainNavbar pathname={pathname} />

        <NotificationBadge />

        <Nav pullRight className="header-meta-nav">
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

          <HelpMenu />

          <LinkContainer relativeActive to={Routes.WELCOME}>
            <NavItem id="welcome-nav-link" aria-label="Welcome">
              <Icon size="lg" fixedWidth title="Welcome" name="home" />
            </NavItem>
          </LinkContainer>

          <UserMenu />
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
