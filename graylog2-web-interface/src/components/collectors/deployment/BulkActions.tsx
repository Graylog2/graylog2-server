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
import { ConfirmDialog } from 'components/common';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

import { useCollectorsMutations } from '../hooks';

const BulkActions = () => {
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const { bulkDeleteEnrollmentTokens } = useCollectorsMutations();
  const [showConfirm, setShowConfirm] = useState(false);

  const handleConfirm = useCallback(async () => {
    await bulkDeleteEnrollmentTokens(selectedEntities);
    setSelectedEntities([]);
    setShowConfirm(false);
  }, [selectedEntities, bulkDeleteEnrollmentTokens, setSelectedEntities]);

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
          Are you sure you want to delete {selectedEntities.length} enrollment tokens?
          New collectors will not be able to enroll using the deleted tokens.
        </ConfirmDialog>
      )}
    </>
  );
};

export default BulkActions;
