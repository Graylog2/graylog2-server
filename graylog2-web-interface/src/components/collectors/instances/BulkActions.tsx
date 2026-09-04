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
import { useState, useCallback } from 'react';

import { MenuItem } from 'components/bootstrap';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

import ReassignFleetModal from './ReassignFleetModal';

import { useFleets, useCollectorPermissions } from '../hooks';

/**
 * Whether this component has any action to offer the current user.
 *
 * Exported so a table can decide whether to render the bulk-select column at all -- an
 * `EntityDataTable` shows that column whenever `bulkSelection.actions` is set, and cannot tell
 * that the element it was handed renders nothing. Callers get a boolean and stay unaware of which
 * actions exist or which permissions they need.
 *
 * Reassigning needs a fleet to move *to*, so the answer is "is there any fleet the user may assign
 * into"; `isEntitySelectable` separately gates which instances may be moved *from*. While the
 * fleet list is still loading we assume yes, so the column does not pop in for the common,
 * permitted case.
 */
export const useHasBulkActions = () => {
  const { data: fleets } = useFleets();
  const { canAssignToFleet } = useCollectorPermissions();

  return fleets === undefined || fleets.some((fleet) => canAssignToFleet(fleet.id));
};

const BulkActions = () => {
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const [showReassignModal, setShowReassignModal] = useState(false);
  const canReassignAnywhere = useHasBulkActions();

  const toggleReassignModal = useCallback(() => {
    setShowReassignModal((cur) => !cur);
  }, []);

  const handleReassignSuccess = useCallback(() => {
    setSelectedEntities([]);
  }, [setSelectedEntities]);

  return (
    <>
      {canReassignAnywhere && (
        <BulkActionsDropdown>
          <MenuItem onSelect={toggleReassignModal}>Reassign to fleet</MenuItem>
        </BulkActionsDropdown>
      )}
      {showReassignModal && (
        <ReassignFleetModal
          origin="bulk-selection"
          instanceUids={selectedEntities}
          onClose={toggleReassignModal}
          onSuccess={handleReassignSuccess}
        />
      )}
    </>
  );
};

export default BulkActions;
