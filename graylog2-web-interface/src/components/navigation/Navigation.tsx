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
import styled, { css } from 'styled-components';

import useLocation from 'routing/useLocation';
import { Link, LinkContainer } from 'components/common/router';
import AppConfig from 'util/AppConfig';
import { Navbar, Nav } from 'components/bootstrap';
import GlobalThroughput from 'components/throughput/GlobalThroughput';
import Routes from 'routing/Routes';
import BrandNavLogo from 'components/perspectives/NavigationBrand';
import usePluginEntities from 'hooks/usePluginEntities';
import MainNavbar from 'components/navigation/MainNavbar';
import { FEATURE_FLAG } from 'components/quick-jump/Constants';
import { NAV_ITEM_HEIGHT } from 'theme/constants';

import UserMenu from './UserMenu';
import HelpMenu from './HelpMenu';
import NotificationBadge from './NotificationBadge';
import DevelopmentHeaderBadge from './DevelopmentHeaderBadge';
import InactiveNavItem from './InactiveNavItem';
import ScratchpadToggle from './ScratchpadToggle';
import StyledNavbar from './Navigation.styles';

import { QuickJumpModalContainer } from '../quick-jump';

type Props = {
  pathname: string;
};

const BrandLink = styled(Link)(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    min-height: ${NAV_ITEM_HEIGHT};
    color: ${theme.colors.text.primary};

    &:hover,
    &:active,
    &:focus {
      text-decoration: none;
      color: ${theme.colors.text.primary};
    }
  `,
);

const Navigation = React.memo(({ pathname }: Props) => {
  const pluginItems = usePluginEntities('navigationItems');

  return (
    <StyledNavbar fluid fixedTop collapseOnSelect>
      <Navbar.Header>
        <Navbar.Brand>
          <BrandLink to={Routes.WELCOME} aria-label="Welcome">
            <BrandNavLogo />
          </BrandLink>
        </Navbar.Brand>
        <Navbar.Toggle />
        <DevelopmentHeaderBadge smallScreen />
        {pluginItems.map(({ key, component: Item }) => (
          <Item key={key} smallScreen />
        ))}
      </Navbar.Header>
      <Navbar.Collapse>
        <MainNavbar pathname={pathname} />

        <NotificationBadge />

        <Nav pullRight className="header-meta-nav">
          {AppConfig.isFeatureEnabled(FEATURE_FLAG) ? <QuickJumpModalContainer /> : null}

          {AppConfig.isCloud() ? (
            <GlobalThroughput disabled />
          ) : (
            <LinkContainer to={Routes.SYSTEM.CLUSTER.NODES}>
              <GlobalThroughput />
            </LinkContainer>
          )}

          <InactiveNavItem className="dev-badge-wrap">
            <DevelopmentHeaderBadge />
            {pluginItems.map(({ key, component: Item }) => (
              <Item key={key} />
            ))}
          </InactiveNavItem>
          <ScratchpadToggle />

          <HelpMenu />

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
