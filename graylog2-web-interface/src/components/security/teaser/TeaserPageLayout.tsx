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
import type { PropsWithChildren } from 'react';
import { useState } from 'react';

import { SideNav, SideNavItem, ContentArea, Container } from 'components/security/page-layout';
import Routes from 'routing/Routes';

const navItems = [
  {
    path: Routes.SECURITY.OVERVIEW,
    iconName: 'poll',
    end: true,
    title: 'Overview',
  },
  {
    path: Routes.SECURITY.USER_ACTIVITY,
    iconName: 'user',
    end: false,
    title: 'User Activity',
  },
  {
    path: Routes.SECURITY.HOST_ACTIVITY,
    iconName: 'tv',
    end: false,
    title: 'Host Activity',
  },
  {
    path: Routes.SECURITY.NETWORK_ACTIVITY,
    iconName: 'wifi',
    end: false,
    title: 'Network Activity',
  },
  {
    path: Routes.SECURITY.ANOMALIES,
    iconName: 'search-plus',
    end: false,
    title: 'Anomalies',
  },
] as const;

const TeaserPageLayout = ({ children }: PropsWithChildren) => {
  const [showSideBar, setShowSideBar] = useState(true);

  return (
    <Container>
      <SideNav isOpen={showSideBar} toggleIsOpen={() => setShowSideBar((cur) => !cur)}>
        {navItems.map((route) => (
          <SideNavItem key={route.title}
                       iconName={route.iconName}
                       linkTarget={route.path}
                       linkEnd={route.end}>
            {route.title}
          </SideNavItem>
        ))}
      </SideNav>
      <ContentArea $sideNavIsOpen={showSideBar}>
        {children}
      </ContentArea>
    </Container>
  );
};

export default TeaserPageLayout;
