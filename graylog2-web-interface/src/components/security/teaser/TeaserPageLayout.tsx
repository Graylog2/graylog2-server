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
import styled from 'styled-components';
import type { PropsWithChildren } from 'react';
import { useState } from 'react';

import { Alert, Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import { SideNav, SideNavItem, ContentArea, Container } from 'components/security/page-layout';
import Routes from 'routing/Routes';

const StyledAlert = styled(Alert)`
  padding: ${({ theme }) => theme.spacings.lg};
  margin: ${({ theme }) => theme.spacings.md};
  margin-top: 0;
`;

const Banner = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
`;

const LeftItems = styled.div`
  display: flex;
  flex-direction: row;
  align-items: baseline;
  gap: ${({ theme }) => theme.spacings.md};
`;

const BoldText = styled.h1`
  font-weight: bold;
  color: ${({ theme }) => theme.colors.variant.danger};
`;

const navItems = [
  {
    path: Routes.SECURITY.OVERVIEW,
    iconName: 'ballot',
    end: true,
    title: 'Overview',
  },
  {
    path: Routes.SECURITY.USER_ACTIVITY,
    iconName: 'person',
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
    iconName: 'zoom_in',
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
        <StyledAlert bsStyle="info" noIcon>
          <Banner>
            <LeftItems>
              <BoldText>Security Demo</BoldText>
              <span>For more information and booking a full demo of the product visit Graylog website.</span>
            </LeftItems>
            <Button bsStyle="primary" role="link" target="_blank" href="https://graylog.org/products/security">
              Graylog Security <Icon name="open_in_new" />
            </Button>
          </Banner>
        </StyledAlert>
        {children}
      </ContentArea>
    </Container>
  );
};

export default TeaserPageLayout;
