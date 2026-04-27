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
import { useState, useMemo, useCallback } from 'react';

import PaginatedEntityTable from 'components/common/PaginatedEntityTable';

import customColumnRenderers from './ColumnRenderers';
import InstanceActions from './InstanceActions';
import BulkActions from './BulkActions';
import { DEFAULT_LAYOUT } from './Constants';
import { InstanceDetailDrawer } from './index';

import type { CollectorInstanceView } from '../types';
import { fetchPaginatedInstances, instancesKeyFn, useFleets, useSources, useDefaultInstanceFilters } from '../hooks';

const CollectorsInstances = () => {
  const [selectedInstance, setSelectedInstance] = useState<CollectorInstanceView | null>(null);
  const { data: fleets } = useFleets();
  const { data: sources } = useSources(selectedInstance?.fleet_id);
  const defaultFilters = useDefaultInstanceFilters();

  const fleetNames = useMemo(() => Object.fromEntries((fleets ?? []).map((fleet) => [fleet.id, fleet.name])), [fleets]);

  const columnRenderers = useMemo(() => customColumnRenderers({ fleetNames }), [fleetNames]);

  const entityActions = useCallback(
    (instance: CollectorInstanceView) => <InstanceActions instance={instance} onDetailsClick={setSelectedInstance} />,
    [],
  );

  return (
    <>
      <PaginatedEntityTable<CollectorInstanceView>
        humanName="instances"
        entityActions={entityActions}
        tableLayout={DEFAULT_LAYOUT}
        fetchEntities={fetchPaginatedInstances}
        keyFn={instancesKeyFn}
        entityAttributesAreCamelCase={false}
        columnRenderers={columnRenderers}
        defaultFilters={defaultFilters}
        bulkSelection={{ actions: <BulkActions /> }}
      />

      {selectedInstance && (
        <InstanceDetailDrawer
          instance={selectedInstance}
          sources={sources ?? []}
          fleetName={fleetNames[selectedInstance.fleet_id] ?? selectedInstance.fleet_id}
          onClose={() => setSelectedInstance(null)}
        />
      )}
    </>
  );
};

export default CollectorsInstances;
