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
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import ReassignFleetModal from './ReassignFleetModal';

import collectorLogsUrl from '../common/collectorLogsUrl';
import { useCollectorsMutations } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { CollectorInstanceView } from '../types';

type Props = {
  instance: CollectorInstanceView;
  onDetailsClick: (instance: CollectorInstanceView) => void;
};

const InstanceActions = ({ instance, onDetailsClick }: Props) => {
  const [showReassignModal, setShowReassignModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const { deleteInstance } = useCollectorsMutations();
  const sendTelemetry = useSendCollectorsTelemetry();

  const handleConfirmDelete = useCallback(async () => {
    await deleteInstance(instance.instance_uid);
    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.DELETED, {
      app_action_value: 'instance-delete',
      instance_id: instance.instance_uid,
      fleet_id: instance.fleet_id,
      status: instance.status,
    });
    setShowDeleteConfirm(false);
  }, [instance, deleteInstance, sendTelemetry]);

  return (
    <>
      <ButtonToolbar>
        <LinkContainer to={collectorLogsUrl(instance.instance_uid)}>
          <Button
            bsSize="xsmall"
            onClick={() =>
              sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.VIEW_LOGS_CLICKED, {
                app_action_value: 'instance-view-logs',
                instance_id: instance.instance_uid,
                fleet_id: instance.fleet_id,
              })
            }>
            View Logs
          </Button>
        </LinkContainer>
        <Button
          bsSize="xsmall"
          onClick={() => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.DETAILS_OPENED, {
              app_action_value: 'instance-details',
              instance_id: instance.instance_uid,
              fleet_id: instance.fleet_id,
              status: instance.status,
            });
            onDetailsClick(instance);
          }}>
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
