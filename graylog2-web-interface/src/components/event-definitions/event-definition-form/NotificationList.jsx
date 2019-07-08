import React from 'react';
import PropTypes from 'prop-types';
import { Button, Col, Row } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { DataTable, Spinner } from 'components/common';

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

  handleRemoveClick = (notificationId) => {
    return () => {
      const { onRemoveNotificationClick } = this.props;
      onRemoveNotificationClick(notificationId);
    };
  };

  notificationFormatter = (notification) => {
    // Guard in case it is a new Notification, but its information has not been loaded yet
    if (!notification) {
      return (
        <tr>
          <td><Spinner text="Loading Notification information..." /></td>
        </tr>
      );
    }
    const plugin = this.getNotificationPlugin(notification.config.type);

    return (
      <tr key={notification.id}>
        <td>{notification.title}</td>
        <td>{plugin.displayName || notification.config.type}</td>
        <td className="actions">
          <Button bsStyle="info" bsSize="xsmall" onClick={this.handleRemoveClick(notification.id)}>
            Remove from Event
          </Button>
        </td>
      </tr>
    );
  };

  render() {
    const { eventDefinition, notifications, onAddNotificationClick } = this.props;

    const definitionNotifications = eventDefinition.notifications
      .map(edn => notifications.find(n => n.id === edn.notification_id));

    if (definitionNotifications.length === 0) {
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
                     rows={definitionNotifications}
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
