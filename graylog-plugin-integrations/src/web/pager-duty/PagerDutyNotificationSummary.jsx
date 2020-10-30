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
