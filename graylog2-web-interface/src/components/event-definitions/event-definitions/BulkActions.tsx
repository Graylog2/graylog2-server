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
import { useCallback, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { ConfirmDialog } from 'components/common';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import { Button } from 'components/bootstrap';
import StringUtils from 'util/StringUtils';

type Props = {
  selectedDefintionsIds: Array<string>,
  setSelectedEventDefinitionsIds: (definitionIds: Array<string>) => void
};
const ACTION_TYPES = {
  DELETE: 'delete',
  DISABLE: 'disable',
  ENABLE: 'enable',
};
const getDescriptor = (count: number) => StringUtils.pluralize(count, 'event definition', 'event definitions');

const ACTION_TEXT = {
  [ACTION_TYPES.DELETE]: {
    dialogTitle: 'Delete Event Definitions',
    dialogBody: (count: number) => `Are you sure you want to delete ${getDescriptor(count)}"?`,
    bulkActionUrl: ApiRoutes.EventDefinitionsApiController.bulkDelete().url,

  },
  [ACTION_TYPES.DISABLE]: {
    dialogTitle: 'Disable Event Definitions',
    dialogBody: (count: number) => `Are you sure you want to disable ${getDescriptor(count)}"?`,
    bulkActionUrl: ApiRoutes.EventDefinitionsApiController.bulkUnschedule().url,
  },
  [ACTION_TYPES.ENABLE]: {
    dialogTitle: 'Enable Event Definitions',
    dialogBody: (count: number) => `Are you sure you want to enable ${getDescriptor(count)}"?`,
    bulkActionUrl: ApiRoutes.EventDefinitionsApiController.bulkSchedule().url,
  },
};

const BulkActions = ({ selectedDefintionsIds, setSelectedEventDefinitionsIds }: Props) => {
  const queryClient = useQueryClient();
  const [showDialog, setShowDialog] = useState(false);
  const [actionType, setActionType] = useState(null);
  const selectedItemsAmount = selectedDefintionsIds?.length;
  const refetchEventDefinitions = useCallback(() => queryClient.invalidateQueries(['eventDefinition', 'overview']), [queryClient]);

  const updateState = ({ show, type }) => {
    setShowDialog(show);
    setActionType(type);
  };

  const handleAction = (action) => {
    switch (action) {
      case ACTION_TYPES.DELETE:
        updateState({ show: true, type: ACTION_TYPES.DELETE });

        break;
      case ACTION_TYPES.ENABLE:
        updateState({ show: true, type: ACTION_TYPES.ENABLE });

        break;
      case ACTION_TYPES.DISABLE:
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
    fetch('POST',
      qualifyUrl(ACTION_TEXT[actionType].bulkActionUrl),
      { entity_ids: selectedDefintionsIds },
    ).then(({ failures }) => {
      if (failures?.length) {
        const notUpdatedDefinitionIds = failures.map(({ entity_id }) => entity_id);
        setSelectedEventDefinitionsIds(notUpdatedDefinitionIds);
        UserNotification.error(`${notUpdatedDefinitionIds.length} out of ${selectedItemsAmount} selected ${getDescriptor(selectedItemsAmount)} could not be ${actionType}d.`);
      } else {
        setSelectedEventDefinitionsIds([]);
        UserNotification.success(`${selectedItemsAmount} ${getDescriptor(selectedItemsAmount)} ${StringUtils.pluralize(selectedItemsAmount, 'was', 'were')} ${actionType}d successfully.`, 'Success');
      }
    })
      .catch((error) => {
        UserNotification.error(`An error occurred while ${actionType} event definition. ${error}`);
      }).finally(() => {
        refetchEventDefinitions();
      });
  }, [actionType, refetchEventDefinitions, selectedDefintionsIds, selectedItemsAmount, setSelectedEventDefinitionsIds]);

  const handleConfirm = () => {
    onAction();
  };

  return (
    <>
      <Button bsSize="xsmall" bsStyle="primary" onClick={() => handleAction(ACTION_TYPES.ENABLE)}>Enable</Button>
      <Button bsSize="xsmall" bsStyle="primary" onClick={() => handleAction(ACTION_TYPES.DISABLE)}>Disable</Button>
      <Button bsSize="xsmall" bsStyle="danger" onClick={() => handleAction(ACTION_TYPES.DELETE)}>Delete</Button>
      {showDialog && (
      <ConfirmDialog title={ACTION_TEXT[actionType]?.dialogTitle}
                     show
                     onConfirm={handleConfirm}
                     onCancel={handleClearState}>
        {ACTION_TEXT[actionType]?.dialogBody(selectedItemsAmount)}
      </ConfirmDialog>
      )}
    </>
  );
};

export default BulkActions;
