import React from 'react';
import PropTypes from 'prop-types';

import CommonNotificationSummary from 'components/event-notifications/event-notification-types/CommonNotificationSummary';

class SlackNotificationSummary extends React.Component {
    static propTypes = {
      type: PropTypes.string.isRequired,
      notification: PropTypes.object,
      definitionNotification: PropTypes.object.isRequired,
    };

    static defaultProps = {
      notification: {},
    };

    render() {
      const { notification } = this.props;

      return (
        <CommonNotificationSummary {...this.props}>
          <>
            <tr>
              <td>Color</td>
              <td>{notification.config.color}</td>
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
              <td>Backlog Item Message</td>
              <td>{notification.config.backlog_item_message}</td>
            </tr>
            <tr>
              <td>User Name</td>
              <td>{notification.config.user_name}</td>
            </tr>
            <tr>
              <td>Notify Channel</td>
              <td>{notification.config.notify_channel}</td>
            </tr>
            <tr>
              <td>Link Names</td>
              <td>{notification.config.link_names}</td>
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
            <tr>
              <td>Proxy</td>
              <td>{notification.config.proxy}</td>
            </tr>
          </>
        </CommonNotificationSummary>
      );
    }
}

export default SlackNotificationSummary;
