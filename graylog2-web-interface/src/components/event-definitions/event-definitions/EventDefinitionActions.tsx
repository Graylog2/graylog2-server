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
import { useQueryClient } from '@tanstack/react-query';

import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';
import { LinkContainer, IfPermitted, ShareButton, ConfirmDialog } from 'components/common';
import { ButtonToolbar, MenuItem, DeleteMenuItem } from 'components/bootstrap';
import useGetPermissionsByScope from 'hooks/useScopePermissions';
import {
  copyEventDefinition,
  deleteEventDefinition,
  enableEventDefinition,
  disableEventDefinition,
  EVENT_DEFINITIONS_QUERY_KEY,
} from 'components/event-definitions/hooks/useEventDefinitions';
import EntityShareModal from 'components/permissions/EntityShareModal';
import UserNotification from 'util/UserNotification';
import { getPathnameWithoutId } from 'util/URLUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import { MoreActions } from 'components/common/EntityDataTable';
import usePluggableEntitySharedActions from 'hooks/usePluggableEntitySharedActions';

import type { EventDefinition } from '../event-definitions-types';
import { isAggregationEventDefinition, isSystemEventDefinition } from '../event-definitions-types';

type Props = {
  eventDefinition: EventDefinition;
};

const DIALOG_TYPES = {
  COPY: 'copy',
  DELETE: 'delete',
  DISABLE: 'disable',
  ENABLE: 'enable',
} as const;

type DialogType = (typeof DIALOG_TYPES)[keyof typeof DIALOG_TYPES];

const DIALOG_TEXT = {
  [DIALOG_TYPES.COPY]: {
    dialogTitle: 'Copy Event Definition',
    dialogBody: (definitionTitle: string) => `Are you sure you want to create a copy of "${definitionTitle}"?`,
  },
  [DIALOG_TYPES.DELETE]: {
    dialogTitle: 'Delete Event Definition',
    dialogBody: (definitionTitle: string) => `Are you sure you want to delete "${definitionTitle}"?`,
  },
  [DIALOG_TYPES.DISABLE]: {
    dialogTitle: 'Disable Event Definition',
    dialogBody: (definitionTitle: string) => `Are you sure you want to disable "${definitionTitle}"?`,
  },
  [DIALOG_TYPES.ENABLE]: {
    dialogTitle: 'Enable Event Definition',
    dialogBody: (definitionTitle: string) => `Are you sure you want to enable "${definitionTitle}"?`,
  },
};

const EventDefinitionActions = ({ eventDefinition }: Props) => {
  const queryClient = useQueryClient();
  const { deselectEntity } = useSelectedEntities();
  const { scopePermissions } = useGetPermissionsByScope(eventDefinition);
  const [currentDefinition, setCurrentDefinition] = useState<EventDefinition | null>(null);
  const [showDialog, setShowDialog] = useState(false);
  const [dialogType, setDialogType] = useState<DialogType | null>(null);
  const [showEntityShareModal, setShowEntityShareModal] = useState(false);
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();
  const { push } = useHistory();
  const { actions: pluggableActions, actionModals: pluggableActionModals } =
    usePluggableEntitySharedActions<EventDefinition>(eventDefinition, 'event_definition');
  const moreActions = [pluggableActions.length ? pluggableActions : null].filter(Boolean);

  const showActions = (): boolean => scopePermissions?.is_mutable;

  const getDeleteActionTitle = () => {
    if (isSystemEventDefinition(eventDefinition)) {
      return 'System Event Definition cannot be deleted';
    }

    return undefined;
  };

  const updateState = ({
    show,
    type,
    definition,
  }: {
    show: boolean;
    type: DialogType | null;
    definition: EventDefinition | null;
  }) => {
    setShowDialog(show);
    setDialogType(type);

    setCurrentDefinition(definition);
  };

  const handleAction = (action: DialogType, definition: EventDefinition) => {
    switch (action) {
      case DIALOG_TYPES.COPY:
        sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_LIST.ROW_ACTION_COPY_CLICKED, {
          app_pathname: getPathnameWithoutId(pathname),
          app_section: 'event-definition-row',
          app_action_value: 'copy-menuitem',
        });

        updateState({ show: true, type: DIALOG_TYPES.COPY, definition });

        break;
      case DIALOG_TYPES.DELETE:
        sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_LIST.ROW_ACTION_DELETE_CLICKED, {
          app_pathname: getPathnameWithoutId(pathname),
          app_section: 'event-definition-row',
          app_action_value: 'delete-menuitem',
        });

        updateState({ show: true, type: DIALOG_TYPES.DELETE, definition });

        break;
      case DIALOG_TYPES.ENABLE:
        sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_LIST.ROW_ACTION_ENABLE_CLICKED, {
          app_pathname: getPathnameWithoutId(pathname),
          app_section: 'event-definition-row',
          app_action_value: 'enable-menuitem',
        });

        updateState({ show: true, type: DIALOG_TYPES.ENABLE, definition });

        break;
      case DIALOG_TYPES.DISABLE:
        sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_LIST.ROW_ACTION_DISABLE_CLICKED, {
          app_pathname: getPathnameWithoutId(pathname),
          app_section: 'event-definition-row',
          app_action_value: 'disable-menuitem',
        });

        updateState({ show: true, type: DIALOG_TYPES.DISABLE, definition });

        break;
      default:
        break;
    }
  };

  const handleShare = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_LIST.ROW_ACTION_SHARE_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'event-definition-list',
      app_action_value: 'share-button',
    });

    setShowEntityShareModal(true);
  };

  const handleClearState = () => {
    updateState({ show: false, type: null, definition: null });
    queryClient.invalidateQueries({ queryKey: EVENT_DEFINITIONS_QUERY_KEY });
  };

  const handleConfirm = () => {
    switch (dialogType) {
      case 'copy':
        copyEventDefinition(currentDefinition)
          .catch(() => {
            // Error feedback is handled by `copyEventDefinition` itself.
          })
          .finally(() => {
            handleClearState();
          });

        break;
      case 'delete':
        deleteEventDefinition(currentDefinition)
          .then(
            () => {
              deselectEntity(currentDefinition.id);

              UserNotification.success(
                'Event Definition deleted successfully',
                `Event Definition "${eventDefinition.title}" was deleted successfully.`,
              );
            },
            (error) => {
              const errorStatus = error?.additional?.body?.errors?.dependency.join(' ') || error;

              UserNotification.error(
                `Deleting Event Definition "${eventDefinition.title}" failed with status: ${errorStatus}`,
                'Could not delete Event Definition',
              );
            },
          )
          .finally(() => {
            handleClearState();
          });

        break;
      case 'enable':
        enableEventDefinition(currentDefinition)
          .catch(() => {
            // Error feedback is handled by `enableEventDefinition` itself.
          })
          .finally(() => {
            handleClearState();
          });

        break;
      case 'disable':
        disableEventDefinition(currentDefinition)
          .catch(() => {
            // Error feedback is handled by `disableEventDefinition` itself.
          })
          .finally(() => {
            handleClearState();
          });

        break;
      default:
        break;
    }
  };

  const onEditEventDefinition = () => push(Routes.ALERTS.DEFINITIONS.edit(eventDefinition.id));

  const isEnabled = eventDefinition?.state === 'ENABLED';

  return (
    <>
      <ButtonToolbar key={`actions-${eventDefinition.id}`}>
        <ShareButton
          entityId={eventDefinition.id}
          entityType="event_definition"
          onClick={handleShare}
          bsSize="xsmall"
        />
        <MoreActions>
          <IfPermitted permissions={`eventdefinitions:edit:${eventDefinition.id}`}>
            <MenuItem onClick={onEditEventDefinition} data-testid="edit-button">
              Edit
            </MenuItem>
          </IfPermitted>
          <IfPermitted permissions="eventdefinitions:create">
            {!isSystemEventDefinition(eventDefinition) && (
              <MenuItem onClick={() => handleAction(DIALOG_TYPES.COPY, eventDefinition)}>Duplicate</MenuItem>
            )}
            <MenuItem divider />
          </IfPermitted>
          <IfPermitted permissions={`eventdefinitions:edit:${eventDefinition.id}`}>
            <MenuItem
              disabled={isSystemEventDefinition(eventDefinition)}
              title={
                isSystemEventDefinition(eventDefinition) ? 'System Event Definition cannot be disabled' : undefined
              }
              onClick={
                isSystemEventDefinition(eventDefinition)
                  ? undefined
                  : () => handleAction(isEnabled ? DIALOG_TYPES.DISABLE : DIALOG_TYPES.ENABLE, eventDefinition)
              }>
              {isEnabled ? 'Disable' : 'Enable'}
            </MenuItem>
          </IfPermitted>
          {showActions() && (
            <IfPermitted permissions={`eventdefinitions:delete:${eventDefinition.id}`}>
              <MenuItem divider />
              <DeleteMenuItem
                disabled={isSystemEventDefinition(eventDefinition)}
                title={getDeleteActionTitle()}
                onClick={
                  isSystemEventDefinition(eventDefinition)
                    ? undefined
                    : () => handleAction(DIALOG_TYPES.DELETE, eventDefinition)
                }
                data-testid="delete-button"
              />
            </IfPermitted>
          )}
          {isAggregationEventDefinition(eventDefinition) && (
            <>
              <IfPermitted
                permissions={[
                  `eventdefinitions:edit:${eventDefinition.id}`,
                  `eventdefinitions:delete:${eventDefinition.id}`,
                ]}
                anyPermissions>
                <MenuItem divider />
              </IfPermitted>
              <LinkContainer to={Routes.ALERTS.DEFINITIONS.replay_search(eventDefinition.id)}>
                <MenuItem>Replay Search</MenuItem>
              </LinkContainer>
            </>
          )}
          {moreActions}
        </MoreActions>
      </ButtonToolbar>
      {showDialog && (
        <ConfirmDialog
          title={DIALOG_TEXT[dialogType].dialogTitle}
          show
          onConfirm={handleConfirm}
          onCancel={handleClearState}>
          {DIALOG_TEXT[dialogType].dialogBody(currentDefinition.title)}
        </ConfirmDialog>
      )}
      {showEntityShareModal && (
        <EntityShareModal
          entityId={eventDefinition.id}
          entityType="event_definition"
          entityTypeTitle="event definition"
          entityTitle={eventDefinition.title}
          description="Search for a User or Team to add as collaborator on this event definition."
          onClose={() => setShowEntityShareModal(false)}
        />
      )}
      {pluggableActionModals}
    </>
  );
};

export default EventDefinitionActions;
