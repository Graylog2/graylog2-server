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
import { useTranslation } from 'react-i18next';
import { useCallback, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { EventsDefinitions } from '@graylog/server-api';

import { ConfirmDialog } from 'components/common';
import UserNotification from 'util/UserNotification';
import { MenuItem, DeleteMenuItem } from 'components/bootstrap';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import { EVENT_DEFINITIONS_QUERY_KEY } from 'components/event-definitions/hooks/useEventDefinitions';

const ACTION_TYPES = {
  DELETE: 'delete',
  DISABLE: 'disable',
  ENABLE: 'enable',
} as const;

type ActionType = (typeof ACTION_TYPES)[keyof typeof ACTION_TYPES];
const BULK_ACTIONS = {
  [ACTION_TYPES.DELETE]: EventsDefinitions.bulkDelete,
  [ACTION_TYPES.DISABLE]: EventsDefinitions.bulkUnschedule,
  [ACTION_TYPES.ENABLE]: EventsDefinitions.bulkSchedule,
};

const BulkActions = () => {
  const { t } = useTranslation('event_definitions');
  const queryClient = useQueryClient();
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const [showDialog, setShowDialog] = useState(false);
  const [actionType, setActionType] = useState<ActionType | null>(null);
  const selectedItemsAmount = selectedEntities?.length;
  const sendTelemetry = useSendTelemetry('event-definition-bulk');
  const refetchEventDefinitions = useCallback(
    () =>
      queryClient.invalidateQueries({
        queryKey: EVENT_DEFINITIONS_QUERY_KEY,
      }),
    [queryClient],
  );

  const updateState = ({ show, type }: { show: boolean; type: ActionType | null }) => {
    setShowDialog(show);
    setActionType(type);
  };

  const handleAction = (action: ActionType) => {
    switch (action) {
      case ACTION_TYPES.DELETE:
        sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_LIST.BULK_ACTION_DELETE_CLICKED, {
          app_action_value: 'delete-menuitem',
        });

        updateState({ show: true, type: ACTION_TYPES.DELETE });

        break;
      case ACTION_TYPES.ENABLE:
        sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_LIST.BULK_ACTION_ENABLE_CLICKED, {
          app_action_value: 'enable-menuitem',
        });

        updateState({ show: true, type: ACTION_TYPES.ENABLE });

        break;
      case ACTION_TYPES.DISABLE:
        sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_LIST.BULK_ACTION_DISABLE_CLICKED, {
          app_action_value: 'disable-menuitem',
        });

        updateState({ show: true, type: ACTION_TYPES.DISABLE });

        break;
      default:
        break;
    }
  };

  const handleClearState = () => {
    updateState({ show: false, type: null });
    refetchEventDefinitions();
  };

  const onAction = useCallback(() => {
    BULK_ACTIONS[actionType]({ entity_ids: selectedEntities })
      .then(({ failures }: { failures: Array<{ entity_id: string }> }) => {
        if (failures?.length) {
          const notUpdatedDefinitionIds = failures.map(({ entity_id }) => entity_id);
          setSelectedEntities(notUpdatedDefinitionIds);
          UserNotification.error(
            t(`bulk_actions.${actionType}.failed`, {
              count: selectedItemsAmount,
              failed: notUpdatedDefinitionIds.length,
            }),
          );
        } else {
          setSelectedEntities([]);
          UserNotification.success(
            t(`bulk_actions.${actionType}.success`, { count: selectedItemsAmount }),
            t('bulk_actions.success_title'),
          );
        }
      })
      .catch((error) => {
        UserNotification.error(t(`bulk_actions.${actionType}.error`, { error }));
      })
      .finally(() => {
        refetchEventDefinitions();
      });
  }, [actionType, refetchEventDefinitions, selectedEntities, selectedItemsAmount, setSelectedEntities, t]);

  const handleConfirm = () => {
    onAction();
    setShowDialog(false);
  };

  return (
    <>
      <BulkActionsDropdown>
        <MenuItem onSelect={() => handleAction(ACTION_TYPES.ENABLE)}>{t('bulk_actions.enable_action')}</MenuItem>
        <MenuItem onSelect={() => handleAction(ACTION_TYPES.DISABLE)}>{t('bulk_actions.disable_action')}</MenuItem>
        <DeleteMenuItem onSelect={() => handleAction(ACTION_TYPES.DELETE)} />
      </BulkActionsDropdown>
      {showDialog && (
        <ConfirmDialog
          title={actionType ? t(`bulk_actions.${actionType}.title`) : undefined}
          show
          onConfirm={handleConfirm}
          onCancel={handleClearState}>
          {actionType ? t(`bulk_actions.${actionType}.confirm`, { count: selectedItemsAmount }) : null}
        </ConfirmDialog>
      )}
    </>
  );
};

export default BulkActions;
