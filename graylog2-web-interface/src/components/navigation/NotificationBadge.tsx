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
import { useQuery } from '@tanstack/react-query';

import { LinkContainer } from 'components/common/router';
import { Badge, Nav } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { NotificationsActions } from 'stores/notifications/NotificationsStore';
import { NAV_ITEM_HEIGHT } from 'theme/constants';

import InactiveNavItem from './InactiveNavItem';

const StyledNav = styled(Nav)`
  > li > a {
    min-height: ${NAV_ITEM_HEIGHT};
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 12px;
  }
`;

const StyledInactiveNavItem = styled(InactiveNavItem)`
  a:hover {
    border: 0;
    text-decoration: none;
  }
`;

type NotificationsResponse = {
  total: number,
};

const POLL_INTERVAL = 3000;
const fetchNotificationCount = () => NotificationsActions.list().then((response: NotificationsResponse) => response.total);

const NotificationBadge = () => {
  const { data: total } = useQuery(['notifications.count'], fetchNotificationCount, {
    refetchInterval: POLL_INTERVAL,
  });

  return total
    ? (
      <StyledNav navbar>
        <LinkContainer to={Routes.SYSTEM.OVERVIEW}>
          <StyledInactiveNavItem>
            <Badge bsStyle="danger" data-testid="notification-badge">{total}</Badge>
          </StyledInactiveNavItem>
        </LinkContainer>
      </StyledNav>
    )
    : null;
};

export default NotificationBadge;
