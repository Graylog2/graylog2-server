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
import React, { useState } from 'react';
import cloneDeep from 'lodash/cloneDeep';
import without from 'lodash/without';

import { LinkContainer, Icon } from 'components/common';
import { Alert, Col, Row, Button } from 'components/bootstrap';
import Routes from 'routing/Routes';
import usePermissions from 'hooks/usePermissions';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import AddNotificationForm from './AddNotificationForm';
import NotificationSettingsForm from './NotificationSettingsForm';
import NotificationList from './NotificationList';
import styles from './NotificationsForm.css';

import commonStyles from '../common/commonStyles.css';

type Props = {
  eventDefinition: any;
  notifications: any[];
  defaults: any;
  onChange: (...args: any[]) => void;
};

const NotificationsForm = ({ eventDefinition, notifications, defaults, onChange }: Props) => {
  const sendTelemetry = useSendTelemetry('event-definition-notifications');
  const { isPermitted } = usePermissions();
  const [showAddNotificationForm, setShowAddNotificationForm] = useState(false);

  const toggleAddNotificationForm = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.ADD_CLICKED, {
      app_action_value: 'add-notification-button',
    });

    setShowAddNotificationForm(!showAddNotificationForm);
  };

  const handleAssignNotification = (nextNotification: string) => {
    const nextNotifications = cloneDeep(eventDefinition.notifications);

    nextNotifications.push({
      notification_id: nextNotification,
    });

    onChange('notifications', nextNotifications);
    toggleAddNotificationForm();
  };

  const handleRemoveNotification = (notificationId: string) => {
    const notification = eventDefinition.notifications.find((n) => n.notification_id === notificationId);
    const nextNotifications = without(eventDefinition.notifications, notification);

    onChange('notifications', nextNotifications);
  };

  const notificationIds = eventDefinition.notifications.map((n) => n.notification_id);
  const missingPermissions = notificationIds.filter((id) => !isPermitted(`eventnotifications:read:${id}`));

  if (missingPermissions.length > 0) {
    return (
      <Row>
        <Col md={6} lg={5}>
          <Alert bsStyle="warning">
            Missing Notifications Permissions for: <br /> {missingPermissions.join(', ')}
          </Alert>
        </Col>
      </Row>
    );
  }

  if (showAddNotificationForm) {
    return (
      <AddNotificationForm
        notifications={notifications}
        onChange={handleAssignNotification}
        onCancel={toggleAddNotificationForm}
        hasCreationPermissions={isPermitted('eventnotifications:create')}
      />
    );
  }

  return (
    <Row>
      <Col md={6} lg={5}>
        <span className={styles.manageNotifications}>
          <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.LIST} target="_blank">
            <Button bsStyle="link" bsSize="small">
              Manage Notifications <Icon name="open_in_new" />
            </Button>
          </LinkContainer>
        </span>
        <h2 className={commonStyles.title}>
          Notifications <small>(optional)</small>
        </h2>
        <p>
          Is this Event important enough that requires your attention? Make it an Alert by adding Notifications to it.
        </p>

        <NotificationList
          eventDefinition={eventDefinition}
          notifications={notifications}
          onAddNotificationClick={toggleAddNotificationForm}
          onRemoveNotificationClick={handleRemoveNotification}
        />
        <NotificationSettingsForm eventDefinition={eventDefinition} defaults={defaults} onSettingsChange={onChange} />
      </Col>
    </Row>
  );
};

export default NotificationsForm;
