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
import usePermissions from 'hooks/usePermissions';
import useNotificationBadgeCount from 'components/notifications/hooks/useNotificationBadgeCount';
import Routes from 'routing/Routes';
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

const NotificationBadge = () => {
  const { isPermitted } = usePermissions();
  const enabled = isPermitted('notifications:read');
  const { data, isLoading } = useNotificationBadgeCount({ enabled });

  return isLoading || !data ? null : (
    <StyledNav navbar>
      <LinkContainer to={Routes.SYSTEM.HEALTH}>
        <StyledInactiveNavItem>
          <Badge bsStyle="danger" data-testid="notification-badge" title="System Notifications">
            {data}
          </Badge>
        </StyledInactiveNavItem>
      </LinkContainer>
    </StyledNav>
  );
};

export default NotificationBadge;
