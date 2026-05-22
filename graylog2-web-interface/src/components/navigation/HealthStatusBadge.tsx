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
import type * as Immutable from 'immutable';

import { Icon, LinkContainer } from 'components/common';
import { Badge, Nav } from 'components/bootstrap';
import useNotifications from 'components/notifications/useNotifications';
import { STATUS_LABELS } from 'components/health/healthStatusCopy';
import { useHealthSummary } from 'components/health/useHealthModule';
import useHealthModuleVisible, { HEALTH_QUERY_PARAM, HEALTH_ON_VALUE } from 'components/health/useHealthModuleVisible';
import type { HealthStatus } from 'components/health/HealthReport.types';
import useCurrentUser from 'hooks/useCurrentUser';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import Routes from 'routing/Routes';
import { NAV_ITEM_HEIGHT } from 'theme/constants';

import InactiveNavItem from './InactiveNavItem';

const hasNotificationPermission = (permissions: Immutable.List<string>): boolean =>
  permissions.some((p) => p === '*' || p === 'notifications:*' || p.startsWith('notifications:read'));

const STATUS_TO_BS_STYLE = {
  healthy: 'success',
  warning: 'warning',
  critical: 'danger',
  unknown: 'default',
} as const satisfies Record<HealthStatus, string>;

const STATUS_TO_ICON = {
  healthy: 'check_circle',
  warning: 'warning',
  critical: 'error',
  unknown: 'help',
} as const satisfies Record<HealthStatus, string>;

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

const BadgeIcon = styled(Icon)`
  margin-right: 4px;
  margin-bottom: 2px;
`;

const StyledBadge = styled(Badge)`
  cursor: pointer;
`;

const HealthStatusBadge = () => {
  const { data: { valid: hasEnterpriseLicense } = { valid: false } } = usePluggableLicenseCheck('/license/enterprise');
  const showHealthModule = useHealthModuleVisible();
  const { overallStatus } = useHealthSummary();
  const currentUser = useCurrentUser();
  const canReadNotifications = hasNotificationPermission(currentUser.permissions);
  const { data: notifications } = useNotifications({ enabled: canReadNotifications });

  if (!hasEnterpriseLicense || !showHealthModule) return null;

  const notificationCount = notifications?.total ?? 0;
  const statusLabel = STATUS_LABELS[overallStatus];
  const overviewWithHealthOn = `${Routes.SYSTEM.OVERVIEW}?${HEALTH_QUERY_PARAM}=${HEALTH_ON_VALUE}`;
  const accessibleLabel =
    notificationCount > 0
      ? `Cluster health: ${statusLabel}, ${notificationCount} system notifications`
      : `Cluster health: ${statusLabel}`;

  return (
    <StyledNav navbar>
      <LinkContainer to={overviewWithHealthOn}>
        <StyledInactiveNavItem>
          <StyledBadge
            bsStyle={STATUS_TO_BS_STYLE[overallStatus]}
            data-testid="health-status-badge"
            title={accessibleLabel}
            aria-label={accessibleLabel}>
            <BadgeIcon name={STATUS_TO_ICON[overallStatus]} size="sm" />
            {notificationCount > 0 ? notificationCount : null}
          </StyledBadge>
        </StyledInactiveNavItem>
      </LinkContainer>
    </StyledNav>
  );
};

export default HealthStatusBadge;
