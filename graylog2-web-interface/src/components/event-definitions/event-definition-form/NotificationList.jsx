import React from 'react';
import PropTypes from 'prop-types';
import { Button, Col, Row } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { DataTable } from 'components/common';
import { NOTIFICATION_TYPE } from 'components/event-notifications/event-notification-types';

class NotificationList extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    notifications: PropTypes.array.isRequired,
    onAddNotificationClick: PropTypes.func.isRequired,
    onRemoveNotificationClick: PropTypes.func.isRequired,
  };

  getNotificationPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventNotificationTypes').find(n => n.type === type);
  };

  notificationFormatter = (notification) => {
    const plugin = this.getNotificationPlugin(notification.config.type);

    return (
      <tr key={notification.id}>
        <td>{notification.title}</td>
        <td>{plugin.displayName || notification.config.type}</td>
        <td className="actions"><Button bsStyle="info" bsSize="xsmall">Remove from Event</Button></td>
      </tr>
    );
  };

  render() {
    const { eventDefinition, notifications, onAddNotificationClick } = this.props;

    const notificationActions = eventDefinition.actions
      .filter(action => action.type === NOTIFICATION_TYPE)
      .map(action => notifications.find(n => n.id === action.notification_id));

    if (notificationActions.length === 0) {
      return (
        <p>
          This Event is not configured to trigger any Notifications yet.{' '}
          <Button className="btn-text" bsStyle="link" bsSize="small" onClick={onAddNotificationClick}>
            Get notified
          </Button>.
        </p>
      );
    }
    return (
      <Row>
        <Col md={8} lg={6}>
          <DataTable id="event-definition-notifications"
                     className="table-striped table-hover"
                     headers={['Notification', 'Type', 'Actions']}
                     sortByKey="title"
                     rows={notificationActions}
                     dataRowFormatter={this.notificationFormatter}
                     filterKeys={[]} />
          <Button bsStyle="success" onClick={onAddNotificationClick}>
            Add Notification
          </Button>
        </Col>
      </Row>
    );
  }
}

export default NotificationList;
