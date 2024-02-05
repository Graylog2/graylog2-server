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

function PagerDutyNotificationSummary({ notification, ...restProps }) {
  return (
    <CommonNotificationSummary {...restProps} notification={notification}>
      <tr>
        <td>Routing Key</td>
        <td><code>{notification?.config?.routing_key}</code></td>
      </tr>
      <tr>
        <td>Use Custom Incident Key</td>
        <td><code>{notification?.config?.custom_incident ? 'Yes' : 'No'}</code></td>
      </tr>
      <tr>
        <td>Incident Key Prefix</td>
        <td><code>{notification?.config?.key_prefix}</code></td>
      </tr>
      <tr>
        <td>Client Name</td>
        <td><code>{notification?.config?.client_name}</code></td>
      </tr>
      <tr>
        <td>Client URL</td>
        <td><code>{notification?.config?.client_url}</code></td>
      </tr>
    </CommonNotificationSummary>
  );
}

PagerDutyNotificationSummary.propTypes = {
  type: PropTypes.string.isRequired,
  notification: PropTypes.shape({
    config: PropTypes.shape({
      routing_key: PropTypes.string,
      custom_incident: PropTypes.bool,
      key_prefix: PropTypes.string,
      client_name: PropTypes.string,
      client_url: PropTypes.string,
    }).isRequired,
  }).isRequired,
  definitionNotification: PropTypes.shape.isRequired,
};

export default PagerDutyNotificationSummary;
