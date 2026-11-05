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
import React from 'react';
import { useTranslation } from 'react-i18next';

import CommonNotificationSummary from 'components/event-notifications/event-notification-types/CommonNotificationSummary';

import type { TeamsNotificationSummaryV2Type } from '../types';

function TeamsNotificationSummary({ notification, ...restProps }: TeamsNotificationSummaryV2Type) {
  const { t } = useTranslation('integrations');

  return (
    <CommonNotificationSummary {...restProps} notification={notification}>
      <tr>
        <td>{t('teams_notification_v2_details.webhook_url_label')}</td>
        <td>{notification.config.webhook_url}</td>
      </tr>

      <tr>
        <td>{t('teams_notification_v2_details.adaptive_card_label')}</td>
        <td>{notification.config.adaptive_card}</td>
      </tr>
      <tr>
        <td>{t('teams_notification_details.time_zone_label')}</td>
        <td>{notification.config.time_zone}</td>
      </tr>
      <tr>
        <td>{t('teams_notification_v2_details.backlog_limit_label')}</td>
        <td>{notification.config.backlog_size}</td>
      </tr>
    </CommonNotificationSummary>
  );
}

export default TeamsNotificationSummary;
