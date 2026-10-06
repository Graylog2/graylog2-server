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
import { ConfirmDialog, IconButton, LinkContainer } from 'components/common';
import { MoreActions } from 'components/common/EntityDataTable';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import ReassignFleetModal from './ReassignFleetModal';

import collectorReceivedMessagesUrl from '../common/collectorReceivedMessagesUrl';
import { AGENT_ID_FIELD } from '../common/fields';
import collectorSystemLogsUrl from '../common/collectorSystemLogsUrl';
import { useCollectorsMutations, useCollectorPermissions } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import { instanceTelemetryProps } from '../hooks/telemetry-helpers';
import type { CollectorInstanceView } from '../types';

type Props = {
  instance: CollectorInstanceView;
  onDetailsClick: (instance: CollectorInstanceView) => void;
};

const InstanceActions = ({ instance, onDetailsClick }: Props) => {
  const [showReassignModal, setShowReassignModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const { deleteInstance } = useCollectorsMutations();
  const { canDeleteInstance, canAssignToFleet, canReadSystemLogs } = useCollectorPermissions();
  const canReassign = canAssignToFleet(instance.fleet_id);
  const canDelete = canDeleteInstance(instance.fleet_id);
  const sendTelemetry = useSendCollectorsTelemetry();

  const handleConfirmDelete = useCallback(async () => {
    await deleteInstance(instance.instance_uid);
    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.DELETED, {
      app_action_value: 'instance-delete',
      ...instanceTelemetryProps(instance),
    });
    setShowDeleteConfirm(false);
  }, [instance, deleteInstance, sendTelemetry]);

  return (
    <>
      <ButtonToolbar>
        <LinkContainer to={collectorReceivedMessagesUrl(AGENT_ID_FIELD, instance.instance_uid)}>
          <IconButton
            name="search"
            title="Received messages"
            bsStyle="default"
            size="xsmall"
            onClick={() =>
              sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.RECEIVED_MESSAGES_CLICKED, {
                app_action_value: 'instance-received-messages',
                ...instanceTelemetryProps(instance),
                // Both this and the detail drawer can reach these; keep them comparable.
                origin: 'row',
              })
            }
          />
        </LinkContainer>
        {canReadSystemLogs && (
          <LinkContainer to={collectorSystemLogsUrl(instance.instance_uid)}>
            <Button
              bsSize="xsmall"
              onClick={() =>
                sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.VIEW_LOGS_CLICKED, {
                  app_action_value: 'instance-view-logs',
                  ...instanceTelemetryProps(instance),
                  origin: 'row',
                })
              }>
              View System Logs
            </Button>
          </LinkContainer>
        )}
        <Button
          bsSize="xsmall"
          onClick={() => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.DETAILS_OPENED, {
              app_action_value: 'instance-details',
              ...instanceTelemetryProps(instance),
            });
            onDetailsClick(instance);
          }}>
          Details
        </Button>
        {(canReassign || canDelete) && (
          <MoreActions>
            {canReassign && <MenuItem onSelect={() => setShowReassignModal(true)}>Reassign to fleet</MenuItem>}
            {canReassign && canDelete && <MenuItem divider />}
            {canDelete && <DeleteMenuItem onSelect={() => setShowDeleteConfirm(true)} />}
          </MoreActions>
        )}
      </ButtonToolbar>
      {showReassignModal && (
        <ReassignFleetModal
          origin="row"
          instanceUids={[instance.instance_uid]}
          currentFleetId={instance.fleet_id}
          onClose={() => setShowReassignModal(false)}
        />
      )}
      {showDeleteConfirm && (
        <ConfirmDialog
          title="Delete Collector instance"
          show
          onConfirm={handleConfirmDelete}
          onCancel={() => setShowDeleteConfirm(false)}>
          Are you sure you want to delete instance <strong>{instance.hostname || instance.instance_uid}</strong>?<br />
          The Collector will need to be re-enrolled to appear again.
        </ConfirmDialog>
      )}
    </>
  );
};

export default InstanceActions;
