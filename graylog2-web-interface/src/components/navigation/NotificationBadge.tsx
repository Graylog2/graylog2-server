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

import { Group, Icon } from 'components/common';
import { Badge } from 'components/bootstrap';
import usePermissions from 'hooks/usePermissions';
import useNotificationBadgeCount from 'components/notifications/hooks/useNotificationBadgeCount';
import Routes from 'routing/Routes';
import StringUtils from 'util/StringUtils';

import NavBadgeItem from './NavBadgeItem';

const MAX_DISPLAYED_COUNT = 99;

const NotificationBadge = () => {
  const { isPermitted } = usePermissions();
  const enabled = isPermitted('notifications:read');
  const { data: count } = useNotificationBadgeCount({ enabled });

  if (!enabled) return null;

  const accessibleLabel =
    count > 0
      ? `${count} unread system ${StringUtils.pluralize(count, 'notification', 'notifications')}`
      : 'No unread system notifications';
  const displayedCount = count > MAX_DISPLAYED_COUNT ? `${MAX_DISPLAYED_COUNT}+` : count;

  return (
    <NavBadgeItem to={Routes.SYSTEM.NOTIFICATIONS}>
      <Badge aria-label={accessibleLabel} data-testid="notification-badge" title={accessibleLabel}>
        <Group component="span" gap={4} wrap="nowrap">
          <Icon name="notifications" size="sm" />
          {count > 0 ? displayedCount : null}
        </Group>
      </Badge>
    </NavBadgeItem>
  );
};

export default NotificationBadge;
