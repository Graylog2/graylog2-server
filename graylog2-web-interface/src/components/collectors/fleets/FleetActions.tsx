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
import { Trans, useTranslation } from 'react-i18next';

import { ButtonToolbar, DeleteMenuItem } from 'components/bootstrap';
import { ConfirmDialog, HoverForHelp, IconButton, LinkContainer } from 'components/common';
import { MoreActions } from 'components/common/EntityDataTable';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import collectorReceivedMessagesUrl from '../common/collectorReceivedMessagesUrl';
import { AGENT_FLEET_ID_FIELD } from '../common/fields';
import { useCollectorsMutations, useCollectorPermissions, useFleetsBulkStats } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { Fleet } from '../types';

type Props = {
  fleet: Fleet;
};

const FleetActions = ({ fleet }: Props) => {
  const { t } = useTranslation('collectors');
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const { deleteFleet } = useCollectorsMutations();
  const { canDeleteFleet } = useCollectorPermissions();
  const canDelete = canDeleteFleet(fleet.id);
  const { data: bulkStats } = useFleetsBulkStats();
  // Unknown until the stats have loaded; the backend refuses the delete in that case anyway.
  const assignedInstances = bulkStats?.fleets.find((stats) => stats.fleet_id === fleet.id)?.assigned_instances;
  const isDeletable = assignedInstances === 0;
  const sendTelemetry = useSendCollectorsTelemetry();

  const handleConfirmDelete = useCallback(async () => {
    await deleteFleet(fleet.id);
    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.FLEET.DELETED, {
      app_action_value: 'fleet-delete',
      fleet_id: fleet.id,
    });
    setShowDeleteConfirm(false);
  }, [fleet.id, deleteFleet, sendTelemetry]);

  return (
    <>
      <ButtonToolbar>
        <LinkContainer to={collectorReceivedMessagesUrl(AGENT_FLEET_ID_FIELD, fleet.id)}>
          <IconButton
            name="search"
            title={t('fleet_actions.received_messages')}
            bsStyle="default"
            size="xsmall"
            onClick={() =>
              sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.FLEET.RECEIVED_MESSAGES_CLICKED, {
                app_action_value: 'fleet-received-messages',
                fleet_id: fleet.id,
              })
            }
          />
        </LinkContainer>
        {canDelete && (
          <MoreActions>
            <DeleteMenuItem onSelect={() => setShowDeleteConfirm(true)} disabled={!isDeletable}>
              {t('fleet_actions.delete')}
              {assignedInstances > 0 && (
                <HoverForHelp displayLeftMargin>
                  {t('fleet_actions.delete_blocked', { count: assignedInstances })}
                </HoverForHelp>
              )}
            </DeleteMenuItem>
          </MoreActions>
        )}
      </ButtonToolbar>
      {showDeleteConfirm && (
        <ConfirmDialog
          title={t('fleet_actions.delete_title')}
          show
          onConfirm={handleConfirmDelete}
          onCancel={() => setShowDeleteConfirm(false)}>
          <Trans
            t={t}
            i18nKey="fleet_actions.delete_confirm"
            values={{ fleetName: fleet.name }}
            components={{ emphasis: <strong /> }}
          />
        </ConfirmDialog>
      )}
    </>
  );
};

export default FleetActions;
