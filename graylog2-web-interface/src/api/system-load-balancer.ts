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
import { ClusterLoadBalancers } from '@graylog/server-api';

import i18n from 'i18n';
import UserNotification from 'util/UserNotification';

export const overrideLoadBalancerStatus = (nodeId: string, status: string): Promise<void> =>
  ClusterLoadBalancers.override(nodeId, status).then(
    () => {
      UserNotification.success(i18n.t('notifications.lb_change_success', { ns: 'system', status, nodeId }));
    },
    (error: unknown) => {
      UserNotification.error(
        i18n.t('notifications.lb_change_failed', { ns: 'system', nodeId, error }),
        i18n.t('notifications.lb_change_failed_title', { ns: 'system', status, nodeId }),
      );
    },
  );
