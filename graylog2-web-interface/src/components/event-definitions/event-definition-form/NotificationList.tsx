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
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Button } from 'components/bootstrap';
import { DataTable } from 'components/common';

const getNotificationPlugin = (type) => {
  if (type === undefined) {
    return undefined;
  }

  return PluginStore.exports('eventNotificationTypes').find((n) => n.type === type);
};

type NotificationListProps = {
  eventDefinition: any;
  notifications: any[];
  onAddNotificationClick: (...args: any[]) => void;
  onRemoveNotificationClick: (...args: any[]) => void;
};

class NotificationList extends React.Component<NotificationListProps, {
  [key: string]: any;
}> {
  handleRemoveClick = (notificationId) => () => {
    const { onRemoveNotificationClick } = this.props;

    onRemoveNotificationClick(notificationId);
  };

  notificationFormatter = (notification) => {
    // Guard in case it is a new Notification or the Notification was deleted
    if (notification.missing) {
      return (
        <tr>
          <td colSpan={2}>Could not find information for Notification <em>{notification.title}</em></td>
          <td className="actions">
            <Button bsStyle="danger" bsSize="xsmall" onClick={this.handleRemoveClick(notification.title)}>
              Delete
            </Button>
          </td>
        </tr>
      );
    }

    const plugin = getNotificationPlugin(notification.config.type);

    return (
      <tr key={notification.id}>
        <td>{notification.title}</td>
        <td>{plugin?.displayName || notification.config.type}</td>
        <td className="actions">
          <Button bsStyle="danger" bsSize="xsmall" onClick={this.handleRemoveClick(notification.id)}>
            Delete
          </Button>
        </td>
      </tr>
    );
  };

  render() {
    const { eventDefinition, notifications, onAddNotificationClick } = this.props;

    const definitionNotifications = eventDefinition.notifications
      .map((edn) => notifications.find((n) => n.id === edn.notification_id) || {
        title: edn.notification_id,
        missing: true,
      });
    const addNotificationButton = (
      <Button bsStyle="success" onClick={onAddNotificationClick}>
        Add notification
      </Button>
    );

    if (definitionNotifications.length === 0) {
      return (
        <>
          <p>
            This Event is not configured to trigger any Notifications yet.
          </p>
          {addNotificationButton}
        </>
      );
    }

    return (
      <>
        <DataTable id="event-definition-notifications"
                   className="table-striped table-hover"
                   headers={['Notification', 'Type', 'Actions']}
                   sortByKey="title"
                   rows={definitionNotifications}
                   dataRowFormatter={this.notificationFormatter}
                   filterKeys={[]} />
        {addNotificationButton}
      </>
    );
  }
}

export default NotificationList;
