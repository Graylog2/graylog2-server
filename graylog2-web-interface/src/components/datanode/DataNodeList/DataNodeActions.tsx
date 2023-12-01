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

import type { DataNode } from 'preflight/types';
import {
  ConfirmDialog,
} from 'components/common';
import { MenuItem } from 'components/bootstrap';
import OverlayDropdownButton from 'components/common/OverlayDropdownButton';
import { MORE_ACTIONS_TITLE, MORE_ACTIONS_HOVER_TITLE } from 'components/common/EntityDataTable/Constants';
import Routes from 'routing/Routes';

import { rejoinDataNode, removeDataNode, renewDatanodeCertificate } from '../hooks/useDataNodes';

type Props = {
  dataNode: DataNode,
};
const DIALOG_TYPES = {
  REJOIN: 'rejoin',
  REMOVE: 'remove',
  RENEW_CERT: 'renew',
};
const DIALOG_TEXT = {
  [DIALOG_TYPES.REJOIN]: {
    dialogTitle: 'Reset Datanode',
    dialogBody: (datanode: string) => `Are you sure you want to reset datanode "${datanode}"?`,
  },
  [DIALOG_TYPES.REMOVE]: {
    dialogTitle: 'Remove datanode',
    dialogBody: (datanode: string) => `Are you sure you want to remove datanode "${datanode}"?`,
  },
};

const DataNodeActions = ({ dataNode }: Props) => {
  const [showDialog, setShowDialog] = useState(false);
  const [dialogType, setDialogType] = useState(null);

  const updateState = ({ show, type }) => {
    setShowDialog(show);
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
      default:
        break;
    }
  };

  return (
    <>
      <OverlayDropdownButton title={MORE_ACTIONS_TITLE}
                             bsSize="xsmall"
                             buttonTitle={MORE_ACTIONS_HOVER_TITLE}
                             disabled={false}
                             dropdownZIndex={1000}>
        <MenuItem onSelect={() => Routes.SYSTEM.DATANODES.SHOW(dataNode.node_id)}>Edit</MenuItem>
        <MenuItem onSelect={() => renewDatanodeCertificate(dataNode.node_id)}>Renew certificate</MenuItem>
        <MenuItem onSelect={() => {}}>Restart</MenuItem>
        <MenuItem onSelect={() => handleAction(DIALOG_TYPES.REJOIN)}>Rejoin</MenuItem>
        <MenuItem onSelect={() => handleAction(DIALOG_TYPES.REMOVE)}>Remove</MenuItem>
      </OverlayDropdownButton>
      {showDialog && (
        <ConfirmDialog title={DIALOG_TEXT[dialogType].dialogTitle}
                       show
                       onConfirm={handleConfirm}
                       onCancel={handleClearState}>
          {DIALOG_TEXT[dialogType].dialogBody(dataNode.hostname)}
        </ConfirmDialog>
      )}
    </>
  );
};

export default DataNodeActions;
