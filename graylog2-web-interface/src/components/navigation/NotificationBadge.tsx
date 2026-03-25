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

import { LinkContainer } from 'components/common';
import { Badge, Nav } from 'components/bootstrap';
import useCurrentUser from 'hooks/useCurrentUser';
import useNotifications from 'components/notifications/useNotifications';
import Routes from 'routing/Routes';
import { NAV_ITEM_HEIGHT } from 'theme/constants';
import type * as Immutable from 'immutable';

import InactiveNavItem from './InactiveNavItem';

const hasNotificationPermission = (permissions: Immutable.List<string>): boolean =>
  permissions.some((p) => p === '*' || p === 'notifications:*' || p.startsWith('notifications:read'));

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

const NotificationBadge = () => {
  const currentUser = useCurrentUser();
  const enabled = hasNotificationPermission(currentUser.permissions);
  const { data, isLoading } = useNotifications({ enabled });

  return isLoading || !data || data.total === 0 ? null : (
    <StyledNav navbar>
      <LinkContainer to={Routes.SYSTEM.OVERVIEW}>
        <StyledInactiveNavItem>
          <Badge bsStyle="danger" data-testid="notification-badge" title="System Notifications">
            {data.total}
          </Badge>
        </StyledInactiveNavItem>
      </LinkContainer>
    </StyledNav>
  );
};

export default NotificationBadge;
