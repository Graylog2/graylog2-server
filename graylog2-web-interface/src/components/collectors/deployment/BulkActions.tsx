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

import { DeleteMenuItem } from 'components/bootstrap';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import { ConfirmDialog } from 'components/common';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import { useCollectorsMutations } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';

const BulkActions = () => {
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const { bulkDeleteEnrollmentTokens } = useCollectorsMutations();
  const sendTelemetry = useSendCollectorsTelemetry();
  const [showConfirm, setShowConfirm] = useState(false);

  const handleConfirm = useCallback(async () => {
    await bulkDeleteEnrollmentTokens(selectedEntities);
    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.BULK_DELETED, {
      app_action_value: 'token-bulk-delete',
      count: selectedEntities.length,
    });
    setSelectedEntities([]);
    setShowConfirm(false);
  }, [selectedEntities, bulkDeleteEnrollmentTokens, sendTelemetry, setSelectedEntities]);

  return (
    <>
      <BulkActionsDropdown>
        <DeleteMenuItem onSelect={() => setShowConfirm(true)} />
      </BulkActionsDropdown>
      {showConfirm && (
        <ConfirmDialog
          title="Delete enrollment tokens"
          show
          onConfirm={handleConfirm}
          onCancel={() => setShowConfirm(false)}>
          Are you sure you want to delete {selectedEntities.length} enrollment tokens? New Collectors will not be able
          to enroll using the deleted tokens.
        </ConfirmDialog>
      )}
    </>
  );
};

export default BulkActions;
