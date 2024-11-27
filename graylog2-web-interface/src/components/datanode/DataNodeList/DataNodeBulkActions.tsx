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
import { useState, useRef } from 'react';

import { MenuItem } from 'components/bootstrap';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import ConfirmDialog from 'components/common/ConfirmDialog';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import sleep from 'logic/sleep';

import { bulkRemoveDataNode, bulkStartDataNode, bulkStopDataNode } from '../hooks/useDataNodes';

const DataNodeBulkActions = () => {
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const [showDialogType, setShowDialogType] = useState<'REMOVE'|'STOP'|null>(null);
  const { refetch } = useTableFetchContext();
  const statusTimeout = useRef<ReturnType<typeof setTimeout>>();

  const sleepAndClearTimer = async () => {
    if (statusTimeout.current) {
      clearTimeout(statusTimeout.current);
    }

    statusTimeout.current = await sleep(1000);
  };

  const refetchDatanodes = async () => {
    await sleepAndClearTimer();
    await refetch();
  };

  const handleBulkStartDatanode = async () => {
    await bulkStartDataNode(selectedEntities, setSelectedEntities);
    await refetchDatanodes();
  };

  const CONFIRM_DIALOG = {
    REMOVE: {
      dialogTitle: 'Remove Data Nodes',
      dialogBody: `Are you sure you want to remove the selected ${selectedEntities.length > 1 ? `${selectedEntities.length} Data Nodes` : 'Data Node'}?`,
      handleConfirm: async () => {
        bulkRemoveDataNode(selectedEntities, setSelectedEntities);
        setShowDialogType(null);
        await refetchDatanodes();
      },
    },
    STOP: {
      dialogTitle: 'Stop Data Nodes',
      dialogBody: `Are you sure you want to stop the selected ${selectedEntities.length > 1 ? `${selectedEntities.length} Data Nodes` : 'Data Node'}?`,
      handleConfirm: async () => {
        bulkStopDataNode(selectedEntities, setSelectedEntities);
        setShowDialogType(null);
        await refetchDatanodes();
      },
    },
  };

  return (
    <>
      <BulkActionsDropdown>
        <MenuItem onSelect={handleBulkStartDatanode}>Start</MenuItem>
        <MenuItem onSelect={() => setShowDialogType('STOP')}>Stop</MenuItem>
        <MenuItem onSelect={() => setShowDialogType('REMOVE')}>Remove</MenuItem>
      </BulkActionsDropdown>
      {showDialogType && (
      <ConfirmDialog title={CONFIRM_DIALOG[showDialogType].dialogTitle}
                     show
                     onConfirm={CONFIRM_DIALOG[showDialogType].handleConfirm}
                     onCancel={() => setShowDialogType(null)}>
        {CONFIRM_DIALOG[showDialogType].dialogBody}
      </ConfirmDialog>
      )}
    </>
  );
};

export default DataNodeBulkActions;
