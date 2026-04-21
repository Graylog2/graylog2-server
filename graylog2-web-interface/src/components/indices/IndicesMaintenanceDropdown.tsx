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
import { useCallback, useMemo } from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { ClusterDeflector, SystemDeflector, SystemIndexRanges } from '@graylog/server-api';

import { DATA_TIERING_TYPE } from 'components/indices/data-tiering';
import { ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { defaultOnError } from 'util/conditional/onError';

const _onRecalculateIndexRange = (indexSetId: string) => {
  if (
    // eslint-disable-next-line no-alert
    window.confirm(
      'This will recalculate index ranges for this index set using a background system job. Do you want to proceed?',
    )
  ) {
    defaultOnError(
      SystemIndexRanges.rebuildIndexSet(indexSetId),
      'Could not create a job to start index ranges recalculation',
      'Error starting index ranges recalculation',
    );
  }
};

const _onCycleDeflector = (indexSetId: string) => {
  if (
    // eslint-disable-next-line no-alert
    window.confirm('This will manually cycle the current active write index on this index set. Do you want to proceed?')
  ) {
    defaultOnError(
      ClusterDeflector.cycleByindexSetId(indexSetId).then(() => SystemDeflector.deflector(indexSetId)),
      'Cycling deflector failed',
    );
  }
};

type Props = {
  indexSet: IndexSet;
  indexSetId: string;
};

const IndicesMaintenanceDropdown = ({ indexSet, indexSetId }: Props) => {
  const dataTieringPlugin = PluginStore.exports('dataTiering').find(
    (plugin) => plugin.type === DATA_TIERING_TYPE.HOT_WARM,
  );

  const onCycleDeflector = useCallback(() => _onCycleDeflector(indexSetId), [indexSetId]);
  const onRecalculateIndexRange = useCallback(() => _onRecalculateIndexRange(indexSetId), [indexSetId]);
  const cycleButton = useMemo(
    () =>
      indexSet?.writable ? (
        <MenuItem eventKey="2" onClick={onCycleDeflector}>
          Rotate active write index
        </MenuItem>
      ) : null,
    [indexSet?.writable, onCycleDeflector],
  );

  return (
    <ButtonGroup>
      <DropdownButton bsStyle="info" title="Maintenance" id="indices-maintenance-actions" pullRight>
        <MenuItem eventKey="1" onClick={onRecalculateIndexRange}>
          Recalculate index ranges
        </MenuItem>
        {cycleButton}
        {indexSet?.data_tiering_status?.has_failed_snapshot && dataTieringPlugin && (
          <dataTieringPlugin.DeleteFailedSnapshotMenuItem eventKey="3" indexSetId={indexSetId} />
        )}
      </DropdownButton>
    </ButtonGroup>
  );
};

export default IndicesMaintenanceDropdown;
