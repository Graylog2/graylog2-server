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
import * as React from 'react';
import { useState } from 'react';

import UserNotification from 'util/UserNotification';
import { ConfirmDialog, IfPermitted, ShareButton } from 'components/common';
import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { MenuItem, ButtonToolbar } from 'components/bootstrap';
import type { EventNotification } from 'stores/event-notifications/EventNotificationsStore';
import { EventNotificationsActions } from 'stores/event-notifications/EventNotificationsStore';
import EntityShareModal from 'components/permissions/EntityShareModal';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import MoreActions from 'components/common/EntityDataTable/MoreActions';

type Props = {
  isTestLoading: boolean,
  notification: EventNotification,
  onTest: (notification: EventNotification) => void,
  refetchEventNotification: () => void,
};

const EventNotificationActions = ({ isTestLoading, notification, refetchEventNotification, onTest }: Props) => {
  const { deselectEntity } = useSelectedEntities();
  const [showDialog, setShowDialog] = useState(false);
  const [showShareNotification, setShowShareNotification] = useState(undefined);
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  const onDelete = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.NOTIFICATIONS.ROW_ACTION_DELETE_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'event-notification',
      app_action_value: 'notification-delete',
    });

    setShowDialog(true);
  };

  const handleClearState = () => {
    setShowDialog(false);
    refetchEventNotification();
  };

  const handleDelete = () => {
    EventNotificationsActions.delete(notification).then(
      () => {
        deselectEntity(notification.id);

        UserNotification.success('Event Notification deleted successfully',
          `Event Notification "${notification.title}" was deleted successfully.`);
      },
      (error) => {
        UserNotification.error(`Deleting Event Notification "${notification.title}" failed with status: ${error}`,
          'Could not delete Event Notification');
      },
    ).finally(() => {
      handleClearState();
    });
  };

  return (
    <>
      <ButtonToolbar>
        <ShareButton entityType="notification"
                     entityId={notification.id}
                     onClick={() => setShowShareNotification(notification)}
                     bsSize="xsmall" />

        <MoreActions>

          <IfPermitted permissions={`eventnotifications:edit:${notification.id}`}>
            <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.edit(notification.id)}>
              <MenuItem>
                Edit
              </MenuItem>
            </LinkContainer>
          </IfPermitted>
          <IfPermitted permissions={[`eventnotifications:edit:${notification.id}`, `eventnotifications:delete:${notification.id}`]}
                       anyPermissions>
            <IfPermitted permissions={`eventnotifications:edit:${notification.id}`}>
              <MenuItem disabled={isTestLoading} onClick={() => onTest(notification)}>
                {isTestLoading ? 'Testing...' : 'Test Notification'}
              </MenuItem>
            </IfPermitted>
            <MenuItem divider />
            <IfPermitted permissions={`eventnotifications:delete:${notification.id}`}>
              <MenuItem onClick={onDelete}>Delete</MenuItem>
            </IfPermitted>
          </IfPermitted>
        </MoreActions>

      </ButtonToolbar>
      {showDialog && (
        <ConfirmDialog title="Delete Notification"
                       show
                       onConfirm={handleDelete}
                       onCancel={handleClearState}>
          {`Are you sure you want to delete "${notification.title}"`}
        </ConfirmDialog>
      )}
      {showShareNotification && (
        <EntityShareModal entityId={notification.id}
                          entityType="notification"
                          description="Search for a user or team to add as collaborator on this notification."
                          entityTitle={notification.title}
                          onClose={() => setShowShareNotification(undefined)} />
      )}
    </>
  );
};

export default EventNotificationActions;
