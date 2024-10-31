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

import CommonNotificationSummary from 'components/event-notifications/event-notification-types/CommonNotificationSummary';
import type { SlackNotificationSummaryType } from 'integrations/event-notifications/types';

function SlackNotificationSummary({ notification, ...restProps }: SlackNotificationSummaryType) {
  return (
    <CommonNotificationSummary {...restProps} notification={notification}>
      <tr>
        <td>Color</td>
        <td>{notification?.config?.color}</td>
      </tr>
      <tr>
        <td>Webhook URL</td>
        <td>{notification.config.webhook_url}</td>
      </tr>
      <tr>
        <td>Channel</td>
        <td>{notification.config.channel}</td>
      </tr>
      <tr>
        <td>Include Title</td>
        <td>{notification.config.include_title}</td>
      </tr>
      <tr>
        <td>Custom Message</td>
        <td>{notification.config.custom_message}</td>
      </tr>
      <tr>
        <td>Time Zone</td>
        <td>{notification.config.time_zone}</td>
      </tr>
      <tr>
        <td>Message Backlog Limit</td>
        <td>{notification.config.backlog_size}</td>
      </tr>
      <tr>
        <td>User Name</td>
        <td>{notification.config.user_name}</td>
      </tr>
      <tr>
        <td>Notify Channel</td>
        <td>{notification.config.notify_channel ? 'Yes' : 'No'}</td>
      </tr>
      <tr>
        <td>Link Names</td>
        <td>{notification.config.link_names ? 'Yes' : 'No'}</td>
      </tr>
      <tr>
        <td>Icon URL</td>
        <td>{notification.config.icon_url}</td>
      </tr>
      <tr>
        <td>Icon Emoji</td>
        <td>{notification.config.icon_emoji}</td>
      </tr>
    </CommonNotificationSummary>
  );
}

export default SlackNotificationSummary;
