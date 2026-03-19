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

import { Button, ButtonToolbar, DeleteMenuItem, MenuItem } from 'components/bootstrap';
import { ConfirmDialog, LinkContainer } from 'components/common';
import { MoreActions } from 'components/common/EntityDataTable';

import collectorLogsUrl from '../common/collectorLogsUrl';
import { useCollectorsMutations } from '../hooks';
import type { CollectorInstanceView } from '../types';

import ReassignFleetModal from './ReassignFleetModal';

type Props = {
  instance: CollectorInstanceView;
  onDetailsClick: (instance: CollectorInstanceView) => void;
};

const InstanceActions = ({ instance, onDetailsClick }: Props) => {
  const [showReassignModal, setShowReassignModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const { deleteInstance } = useCollectorsMutations();

  const handleConfirmDelete = useCallback(async () => {
    await deleteInstance(instance.instance_uid);
    setShowDeleteConfirm(false);
  }, [instance.instance_uid, deleteInstance]);

  return (
    <>
      <ButtonToolbar>
        <LinkContainer to={collectorLogsUrl(instance.instance_uid)}>
          <Button bsSize="xsmall">View Logs</Button>
        </LinkContainer>
        <Button bsSize="xsmall" onClick={() => onDetailsClick(instance)}>
          Details
        </Button>
        <MoreActions>
          <MenuItem onSelect={() => setShowReassignModal(true)}>Reassign to fleet</MenuItem>
          <MenuItem divider />
          <DeleteMenuItem onSelect={() => setShowDeleteConfirm(true)} />
        </MoreActions>
      </ButtonToolbar>
      {showReassignModal && (
        <ReassignFleetModal
          instanceUids={[instance.instance_uid]}
          currentFleetId={instance.fleet_id}
          onClose={() => setShowReassignModal(false)}
        />
      )}
      {showDeleteConfirm && (
        <ConfirmDialog
          title="Delete collector instance"
          show
          onConfirm={handleConfirmDelete}
          onCancel={() => setShowDeleteConfirm(false)}>
          Are you sure you want to delete instance <strong>{instance.hostname || instance.instance_uid}</strong>?<br />
          The collector will need to be re-enrolled to appear again.
        </ConfirmDialog>
      )}
    </>
  );
};

export default InstanceActions;
