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
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import { ConfirmDialog, IfPermitted } from 'components/common';
import { Button, MenuItem } from 'components/bootstrap';
import type { DataNode } from 'components/datanode/Types';
import { MoreActions } from 'components/common/EntityDataTable';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import sleep from 'logic/sleep';

import DataNodeLogsDialog from './DataNodeLogsDialog';

import {
  rejoinDataNode,
  removeDataNode,
  renewDatanodeCertificate,
  stopDataNode,
  startDataNode,
} from '../hooks/useDataNodes';

const ActionButton = styled(Button)`
  margin-left: 4px;
`;

type Props = {
  dataNode: DataNode;
  displayAs?: 'dropdown' | 'buttons';
  refetch?: () => void;
};

const DIALOG_TYPES = {
  STOP: 'stop',
  REJOIN: 'rejoin',
  REMOVE: 'remove',
  RENEW_CERT: 'renew',
};

const DIALOG_TEXT = {
  [DIALOG_TYPES.REJOIN]: {
    dialogTitle: 'data_node_actions.rejoin_title',
    dialogBody: 'data_node_actions.rejoin_confirm',
  },
  [DIALOG_TYPES.REMOVE]: {
    dialogTitle: 'data_node_actions.remove_title',
    dialogBody: 'data_node_actions.remove_confirm',
  },
  [DIALOG_TYPES.STOP]: {
    dialogTitle: 'data_node_actions.stop_title',
    dialogBody: 'data_node_actions.stop_confirm',
  },
};

const DataNodeActions = ({ dataNode, refetch = undefined, displayAs = 'dropdown' }: Props) => {
  const { t } = useTranslation('datanode');
  const [showLogsDialog, setShowLogsDialog] = useState(false);
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const [dialogType, setDialogType] = useState(null);
  const context = useTableFetchContext(!!refetch);
  const _refetch = refetch || context?.refetch || (() => {});
  const statusTimeout = useRef<ReturnType<typeof setTimeout>>();

  const sleepAndClearTimer = async () => {
    if (statusTimeout.current) {
      clearTimeout(statusTimeout.current);
    }

    statusTimeout.current = await sleep(1000);
  };

  const refetchDatanodes = async () => {
    await sleepAndClearTimer();
    await _refetch();
  };

  const handleStartDatanode = async () => {
    await startDataNode(dataNode.node_id);
    await refetchDatanodes();
  };

  const openConfirmDialog = (type) => {
    setShowConfirmDialog(true);
    setDialogType(type);
  };

  const handleAction = (action) => {
    switch (action) {
      case DIALOG_TYPES.REJOIN:
        openConfirmDialog(DIALOG_TYPES.REJOIN);

        break;
      case DIALOG_TYPES.REMOVE:
        openConfirmDialog(DIALOG_TYPES.REMOVE);

        break;
      case DIALOG_TYPES.STOP:
        openConfirmDialog(DIALOG_TYPES.STOP);

        break;
      default:
        break;
    }
  };

  const handleClearState = async () => {
    setShowConfirmDialog(false);
    setDialogType(null);
    await refetchDatanodes();
  };

  const handleConfirm = () => {
    switch (dialogType) {
      case 'rejoin':
        rejoinDataNode(dataNode.node_id).then(() => {
          handleClearState();
        });

        break;
      case 'remove':
        removeDataNode(dataNode.node_id).then(() => {
          handleClearState();
        });

        break;
      case 'stop':
        stopDataNode(dataNode.node_id).then(() => {
          handleClearState();
        });

        break;
      default:
        break;
    }
  };

  const isDatanodeRunning = dataNode.datanode_status === 'AVAILABLE';
  const isDatanodeRemoved = dataNode.datanode_status === 'REMOVED';
  const isRemovingDatanode = dataNode.datanode_status === 'REMOVING';

  return (
    <IfPermitted permissions="datanode:start">
      {displayAs === 'dropdown' && (
        <MoreActions>
          <MenuItem onSelect={() => renewDatanodeCertificate(dataNode.node_id)}>
            {t('data_node_actions.renew_certificate')}
          </MenuItem>
          {!isDatanodeRunning && <MenuItem onSelect={handleStartDatanode}>{t('data_node_actions.start')}</MenuItem>}
          {isDatanodeRunning && (
            <MenuItem onSelect={() => handleAction(DIALOG_TYPES.STOP)}>{t('data_node_actions.stop')}</MenuItem>
          )}
          {isDatanodeRemoved && (
            <MenuItem onSelect={() => handleAction(DIALOG_TYPES.REJOIN)}>{t('data_node_actions.rejoin')}</MenuItem>
          )}
          {(!isDatanodeRemoved || isRemovingDatanode) && (
            <MenuItem onSelect={() => handleAction(DIALOG_TYPES.REMOVE)}>{t('data_node_actions.remove')}</MenuItem>
          )}
          <MenuItem
            onSelect={() => {
              setShowLogsDialog(true);
            }}>
            {t('data_node_actions.show_logs')}
          </MenuItem>
        </MoreActions>
      )}
      {displayAs === 'buttons' && (
        <>
          {!isDatanodeRunning && (
            <ActionButton onClick={handleStartDatanode} bsSize="small">
              {t('data_node_actions.start')}
            </ActionButton>
          )}
          {isDatanodeRunning && (
            <ActionButton onClick={() => handleAction(DIALOG_TYPES.STOP)} bsSize="small">
              {t('data_node_actions.stop')}
            </ActionButton>
          )}
          {isDatanodeRemoved && (
            <ActionButton onClick={() => handleAction(DIALOG_TYPES.REJOIN)} bsSize="small">
              {t('data_node_actions.rejoin')}
            </ActionButton>
          )}
          {(!isDatanodeRemoved || isRemovingDatanode) && (
            <ActionButton onClick={() => handleAction(DIALOG_TYPES.REMOVE)} bsSize="small">
              {t('data_node_actions.remove')}
            </ActionButton>
          )}
          <ActionButton
            onClick={() => {
              setShowLogsDialog(true);
            }}
            bsSize="small">
            {t('data_node_actions.show_logs')}
          </ActionButton>
        </>
      )}
      {showConfirmDialog && (
        <ConfirmDialog
          title={t(DIALOG_TEXT[dialogType].dialogTitle)}
          show
          onConfirm={handleConfirm}
          onCancel={handleClearState}>
          {t(DIALOG_TEXT[dialogType].dialogBody, { hostname: dataNode.hostname })}
        </ConfirmDialog>
      )}
      {showLogsDialog && (
        <DataNodeLogsDialog
          show={showLogsDialog}
          hostname={dataNode?.hostname}
          onHide={() => {
            setShowLogsDialog(false);
          }}
        />
      )}
    </IfPermitted>
  );
};

export default DataNodeActions;
