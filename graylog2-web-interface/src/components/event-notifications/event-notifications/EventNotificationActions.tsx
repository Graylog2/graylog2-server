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
import OverlayDropdownButton from 'components/common/OverlayDropdownButton';

type Props = {
  isTestLoading: boolean,
  notification: EventNotification,
  onTest: (notification: EventNotification) => void,
  refetchEventNotification: () => void,
};

const EventNotificationActions = ({ isTestLoading, notification, refetchEventNotification, onTest }: Props) => {
  const [showDialog, setShowDialog] = useState(false);
  const [showShareNotification, setShowShareNotification] = useState(undefined);

  const onDelete = () => {
    setShowDialog(true);
  };

  const handleClearState = () => {
    setShowDialog(false);
    refetchEventNotification();
  };

  const handleDelete = () => {
    EventNotificationsActions.delete(notification).then(
      () => {
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

        <OverlayDropdownButton title="More Actions"
                               bsSize="xsmall"
                               dropdownZIndex={1000}>

          <IfPermitted permissions={`eventnotifications:edit:${notification.id}`}>
            <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.edit(notification.id)}>
              <MenuItem>
                Edit
              </MenuItem>
            </LinkContainer>
          </IfPermitted>
          <IfPermitted permissions={[`eventnotifications:edit:${notification.id}`, `eventnotifications:delete:${notification.id}`]} anyPermissions>
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
        </OverlayDropdownButton>

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
                        description="Search for a User or Team to add as collaborator on this notification."
                        entityTitle={notification.title}
                        onClose={() => setShowShareNotification(undefined)} />
      )}
    </>
  );
};

export default EventNotificationActions;
