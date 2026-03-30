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

const BulkActions = () => {
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const [showReassignModal, setShowReassignModal] = useState(false);

  const toggleReassignModal = useCallback(() => {
    setShowReassignModal((cur) => !cur);
  }, []);

  const handleReassignSuccess = useCallback(() => {
    setSelectedEntities([]);
  }, [setSelectedEntities]);

  return (
    <>
      <BulkActionsDropdown>
        <MenuItem onSelect={toggleReassignModal}>Reassign to fleet</MenuItem>
      </BulkActionsDropdown>
      {showReassignModal && (
        <ReassignFleetModal
          instanceUids={selectedEntities}
          onClose={toggleReassignModal}
          onSuccess={handleReassignSuccess}
        />
      )}
    </>
  );
};

export default BulkActions;
