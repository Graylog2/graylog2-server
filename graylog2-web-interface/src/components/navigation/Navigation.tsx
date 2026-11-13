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
import { Link, LinkContainer } from 'components/common';
import { Nav, Navbar } from 'components/bootstrap';
import AppConfig from 'util/AppConfig';
import GlobalThroughput from 'components/throughput/GlobalThroughput';
import Routes from 'routing/Routes';
import BrandNavLogo from 'components/navigation/NavigationBrand';
import { hoverIndicatorStyles } from 'components/common/NavItemStateIndicator';
import usePluginEntities from 'hooks/usePluginEntities';
import MainNavbar from 'components/navigation/MainNavbar';
import useNavigationCollapse from 'components/navigation/useNavigationCollapse';
import { FEATURE_FLAG } from 'components/quick-jump/Constants';
import { NAV_ITEM_HEIGHT, NAVBAR_GAP } from 'theme/constants';

import UserMenu from './UserMenu';
import HelpMenu from './HelpMenu';
import NotificationBadge from './NotificationBadge';
import DevelopmentHeaderBadge from './DevelopmentHeaderBadge';
import ScratchpadToggle from './ScratchpadToggle';

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
    padding: 0 ${NAVBAR_GAP}px;

    &:hover,
    &:active,
    &:focus {
      text-decoration: none;
      color: ${theme.colors.text.primary};
    }
  `,
);

const Brand = styled.div`
  flex: 0 0 auto;
`;

const Icons = styled.nav(
  ({ theme }) => css`
    margin-left: auto;
    flex: 0 0 auto;

    a:hover,
    a:focus-visible {
      ${hoverIndicatorStyles(theme)}
    }
  `,
);

const MainNavAndNotificationBadge = styled.nav`
  display: flex;
  align-items: center;
`;

const Badges = styled.div`
  display: flex;
  align-items: center;
  flex: 0 0 auto;
`;

const Navigation = React.memo(({ pathname }: Props) => {
  const pluginItems = usePluginEntities('navigationItems');
  const pluginBadges = usePluginEntities('navigation.badges');
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const activePluginBadges = pluginBadges.filter(({ useCondition }) => useCondition());
  const { navbarRef, brandRef, badgesRef, iconsRef, menuRef, collapsed } = useNavigationCollapse();

  return (
    <Navbar ref={navbarRef} role="navigation">
      {collapsed && <MainNavbar pathname={pathname} collapsed={collapsed} menuRef={menuRef} />}
      <Brand ref={brandRef}>
        <BrandLink to={Routes.WELCOME} aria-label="Welcome">
          <BrandNavLogo />
        </BrandLink>
      </Brand>
      <MainNavAndNotificationBadge aria-label="Main">
        {!collapsed && <MainNavbar pathname={pathname} collapsed={collapsed} menuRef={menuRef} />}
        <Badges ref={badgesRef}>
          {activePluginBadges.map(({ key, component: PluginBadge }) => (
            <PluginBadge key={key} />
          ))}
          <NotificationBadge />
        </Badges>
      </MainNavAndNotificationBadge>

      <Icons ref={iconsRef} aria-label="Utility">
        <Nav>
          <li>{AppConfig.isFeatureEnabled(FEATURE_FLAG) ? <QuickJumpModalContainer /> : null}</li>

          <li>
            {AppConfig.isCloud() ? (
              <GlobalThroughput disabled />
            ) : (
              <LinkContainer to={Routes.SYSTEM.CLUSTER.NODES}>
                <GlobalThroughput />
              </LinkContainer>
            )}
          </li>

          <DevelopmentHeaderBadge />

          {pluginItems.map(({ key, component: Item }) => (
            <li key={key}>
              <Item />
            </li>
          ))}

          <ScratchpadToggle />

          <HelpMenu />

          <UserMenu />
        </Nav>
      </Icons>
    </Navbar>
  );
});

const NavigationContainer = () => {
  const { pathname } = useLocation();

  return <Navigation pathname={pathname} />;
};

export default NavigationContainer;
