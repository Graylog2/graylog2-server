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
import { useCallback, useMemo } from 'react';

import { ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import { DeflectorActions } from 'stores/indices/DeflectorStore';
import { IndexRangesActions } from 'stores/indices/IndexRangesStore';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

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
  const onCycleDeflector = useCallback(() => _onCycleDeflector(indexSetId), [indexSetId]);
  const onRecalculateIndexRange = useCallback(() => _onRecalculateIndexRange(indexSetId), [indexSetId]);
  const cycleButton = useMemo(() => (indexSet?.writable ? <MenuItem eventKey="2" onClick={onCycleDeflector}>Rotate active write index</MenuItem> : null), [indexSet?.writable, onCycleDeflector]);

  return (
    <ButtonGroup>
      <DropdownButton bsStyle="info" title="Maintenance" id="indices-maintenance-actions" pullRight>
        <MenuItem eventKey="1" onClick={onRecalculateIndexRange}>Recalculate index ranges</MenuItem>
        {cycleButton}
      </DropdownButton>
    </ButtonGroup>
  );
};

IndicesMaintenanceDropdown.propTypes = {
  indexSetId: PropTypes.string.isRequired,
  indexSet: PropTypes.object.isRequired,
};

export default IndicesMaintenanceDropdown;
