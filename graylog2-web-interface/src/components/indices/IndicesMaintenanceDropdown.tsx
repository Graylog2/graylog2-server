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
import PropTypes from 'prop-types';
import * as React from 'react';
import { useState, useCallback, useMemo } from 'react';

import { ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import { ConfirmDialog } from 'components/common';
import { DeflectorActions } from 'stores/indices/DeflectorStore';
import { IndexRangesActions } from 'stores/indices/IndexRangesStore';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import useDeleteFailedSnapshotMutation from 'components/indices/hooks/useDeleteFailedSnapshotMutation';

const _onRecalculateIndexRange = (indexSetId: string) => {
  // eslint-disable-next-line no-alert
  if (window.confirm('This will recalculate index ranges for this index set using a background system job. Do you want to proceed?')) {
    IndexRangesActions.recalculate(indexSetId);
  }
};

const _onCycleDeflector = (indexSetId: string) => {
  // eslint-disable-next-line no-alert
  if (window.confirm('This will manually cycle the current active write index on this index set. Do you want to proceed?')) {
    DeflectorActions.cycle(indexSetId).then(() => {
      DeflectorActions.list(indexSetId);
    });
  }
};

type Props = {
  indexSet: IndexSet,
  indexSetId: string,
};

const IndicesMaintenanceDropdown = ({ indexSet, indexSetId }: Props) => {
  const [showConfirmDelete, setShowConfirmDelete] = useState(false);
  const { deleteFailedSnapshot } = useDeleteFailedSnapshotMutation(indexSetId);
  const onDeleteSnapshot = () => { setShowConfirmDelete(true); };
  const onConfirmDelete = () => { deleteFailedSnapshot(); setShowConfirmDelete(false); };
  const onCycleDeflector = useCallback(() => _onCycleDeflector(indexSetId), [indexSetId]);
  const onRecalculateIndexRange = useCallback(() => _onRecalculateIndexRange(indexSetId), [indexSetId]);
  const cycleButton = useMemo(() => (indexSet?.writable ? <MenuItem eventKey="2" onClick={onCycleDeflector}>Rotate active write index</MenuItem> : null), [indexSet?.writable, onCycleDeflector]);

  return (
    <>
      <ButtonGroup>
        <DropdownButton bsStyle="info" title="Maintenance" id="indices-maintenance-actions" pullRight>
          <MenuItem eventKey="1" onClick={onRecalculateIndexRange}>Recalculate index ranges</MenuItem>
          {cycleButton}
          {indexSet?.has_failed_snapshot && (<MenuItem eventKey="3" onClick={onDeleteSnapshot}>Delete snapshot</MenuItem>)}
        </DropdownButton>
      </ButtonGroup>
      {showConfirmDelete && (
      <ConfirmDialog show={showConfirmDelete}
                     onConfirm={onConfirmDelete}
                     onCancel={() => setShowConfirmDelete(false)}
                     title={`Delete snapshot${indexSet?.failed_snapshot_name ? ` ${indexSet?.failed_snapshot_name}` : ''}`}
                     btnConfirmText="Delete">
        Are you sure?<br />
        Deleting this snapshot will cause the rollover to warm tier (if enabled) to be retried - if it fails again, check logs for underlying cause.
      </ConfirmDialog>
      )}
    </>
  );
};

IndicesMaintenanceDropdown.propTypes = {
  indexSetId: PropTypes.string.isRequired,
  indexSet: PropTypes.object.isRequired,
};

export default IndicesMaintenanceDropdown;
