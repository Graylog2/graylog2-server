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
import styled from 'styled-components';

import { ConfirmDialog } from 'components/common';
import { BootstrapModalWrapper, Button, ButtonGroup, MenuItem, Modal } from 'components/bootstrap';
import type { DataNode } from 'preflight/types';
import MoreActions from 'components/common/EntityDataTable/MoreActions';

import {
  rejoinDataNode,
  removeDataNode,
  renewDatanodeCertificate,
  stopDataNode,
  startDataNode,
} from '../hooks/useDataNodes';
import useDataNodeLogs from '../hooks/useDataNodeLogs';

const ActionButton = styled(Button)`
  margin-left: 4px;
`;

const LogsContainer = styled.div`
  word-break: break-all;
  overflow-wrap: break-word;
  white-space: pre-wrap;
  max-height: 500px;

  & td {
    min-width: 64px;
    vertical-align: text-top;
    padding-bottom: 4px;
  }
`;

type Props = {
  dataNode: DataNode,
  displayAs?: 'dropdown'|'buttons',
};

const DIALOG_TYPES = {
  STOP: 'stop',
  REJOIN: 'rejoin',
  REMOVE: 'remove',
  RENEW_CERT: 'renew',
};

const DIALOG_TEXT = {
  [DIALOG_TYPES.REJOIN]: {
    dialogTitle: 'Rejoin Data Node',
    dialogBody: (datanode: string) => `Are you sure you want to rejoin Data Node "${datanode}"?`,
  },
  [DIALOG_TYPES.REMOVE]: {
    dialogTitle: 'Remove Data Node',
    dialogBody: (datanode: string) => `Are you sure you want to remove Data Node "${datanode}"?`,
  },
  [DIALOG_TYPES.STOP]: {
    dialogTitle: 'Stop Data Node',
    dialogBody: (datanode: string) => `Are you sure you want to stop Data Node "${datanode}"?`,
  },
};

const DataNodeActions = ({ dataNode, displayAs }: Props) => {
  const logs = useDataNodeLogs(dataNode?.hostname);

  const [showLogsDialog, setShowLogsDialog] = useState(false);
  const [logsType, setLogsType] = useState<'stdout'|'stderr'>('stdout');
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const [dialogType, setDialogType] = useState(null);

  const updateState = ({ show, type }) => {
    setShowConfirmDialog(show);
    setDialogType(type);
  };

  const handleAction = (action) => {
    switch (action) {
      case DIALOG_TYPES.REJOIN:
        updateState({ show: true, type: DIALOG_TYPES.REJOIN });

        break;
      case DIALOG_TYPES.REMOVE:
        updateState({ show: true, type: DIALOG_TYPES.REMOVE });

        break;
      case DIALOG_TYPES.STOP:
        updateState({ show: true, type: DIALOG_TYPES.STOP });

        break;
      default:
        break;
    }
  };

  const handleClearState = () => {
    updateState({ show: false, type: null });
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

  const isDatanodeRunning = dataNode.data_node_status === 'AVAILABLE';
  const isDatanodeRemoved = dataNode.data_node_status === 'REMOVED';
  const isRemovingDatanode = dataNode.data_node_status === 'REMOVING';

  return (
    <>
      {displayAs === 'dropdown' && (
        <MoreActions>
          <MenuItem onSelect={() => renewDatanodeCertificate(dataNode.node_id)}>Renew certificate</MenuItem>
          {!isDatanodeRunning && <MenuItem onSelect={() => startDataNode(dataNode.node_id)}>Start</MenuItem>}
          {isDatanodeRunning && <MenuItem onSelect={() => handleAction(DIALOG_TYPES.STOP)}>Stop</MenuItem>}
          {isDatanodeRemoved && <MenuItem onSelect={() => handleAction(DIALOG_TYPES.REJOIN)}>Rejoin</MenuItem>}
          {(!isDatanodeRemoved || isRemovingDatanode) && <MenuItem onSelect={() => handleAction(DIALOG_TYPES.REMOVE)}>Remove</MenuItem>}
          <MenuItem onSelect={() => setShowLogsDialog(true)}>Show logs</MenuItem>
        </MoreActions>
      )}
      {displayAs === 'buttons' && (
        <>
          {!isDatanodeRunning && <ActionButton onClick={() => startDataNode(dataNode.node_id)} bsSize="small">Start</ActionButton>}
          {isDatanodeRunning && <ActionButton onClick={() => handleAction(DIALOG_TYPES.STOP)} bsSize="small">Stop</ActionButton>}
          {isDatanodeRemoved && <ActionButton onClick={() => handleAction(DIALOG_TYPES.REJOIN)} bsSize="small">Rejoin</ActionButton>}
          {(!isDatanodeRemoved || isRemovingDatanode) && <ActionButton onClick={() => handleAction(DIALOG_TYPES.REMOVE)} bsSize="small">Remove</ActionButton>}
          <ActionButton onClick={() => setShowLogsDialog(true)} bsSize="small">Show logs</ActionButton>
        </>
      )}
      {showConfirmDialog && (
        <ConfirmDialog title={DIALOG_TEXT[dialogType].dialogTitle}
                       show
                       onConfirm={handleConfirm}
                       onCancel={handleClearState}>
          {DIALOG_TEXT[dialogType].dialogBody(dataNode.hostname)}
        </ConfirmDialog>
      )}
      {showLogsDialog && (
        <BootstrapModalWrapper showModal={showLogsDialog}
                               onHide={() => setShowLogsDialog(false)}
                               bsSize="large"
                               backdrop>
          <Modal.Header closeButton>
            <Modal.Title>{dataNode.hostname} logs</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <ButtonGroup>
              <Button active={logsType === 'stdout'} onClick={() => setLogsType('stdout')}>STDOUT</Button>
              <Button active={logsType === 'stderr'} onClick={() => setLogsType('stderr')}>STDERR</Button>
            </ButtonGroup>
            <pre>
              {logs[logsType] ? (
                <LogsContainer>
                  <table>
                    <tbody>{logs[logsType]?.map((log) => (<tr><td>{log}</td></tr>))}</tbody>
                  </table>
                </LogsContainer>
              ) : ('No logs.')}
            </pre>
          </Modal.Body>
        </BootstrapModalWrapper>
      )}
    </>
  );
};

DataNodeActions.defaultProps = {
  displayAs: 'dropdown',
};

export default DataNodeActions;
