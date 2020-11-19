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
import PropTypes from 'prop-types';
import CommonNotificationSummary from 'components/event-notifications/event-notification-types/CommonNotificationSummary';

function SlackNotificationSummary({ notification, ...restProps }) {
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
        <td>Custom Message</td>
        <td>{notification.config.custom_message}</td>
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
      <tr>
        <td>Graylog URL</td>
        <td>{notification.config.graylog_url}</td>
      </tr>
    </CommonNotificationSummary>
  );
}

SlackNotificationSummary.propTypes = {
  type: PropTypes.string.isRequired,
  notification: PropTypes.shape({
    config: PropTypes.shape({
      graylog_url: PropTypes.string,
      icon_emoji: PropTypes.string,
      icon_url: PropTypes.string,
      link_names: PropTypes.string,
      notify_channel: PropTypes.string,
      backlog_size: PropTypes.number,
      user_name: PropTypes.string,
      custom_message: PropTypes.string,
      channel: PropTypes.string,
      webhook_url: PropTypes.string,
      color: PropTypes.string,

    }).isRequired,

  }),
  definitionNotification: PropTypes.object.isRequired,
};

SlackNotificationSummary.defaultProps = {
  notification: {},
};

export default SlackNotificationSummary;
