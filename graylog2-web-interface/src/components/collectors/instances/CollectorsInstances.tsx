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
import type { SearchParams } from 'stores/PaginationTypes';

import {
  fetchPaginatedInstances,
  instancesKeyFn,
  useFleets,
  useSources,
  useDefaultInstanceFilters,
} from '../hooks';
import type { CollectorInstanceView } from '../types';

import customColumnRenderers from './ColumnRenderers';
import InstanceActions from './InstanceActions';
import BulkActions from './BulkActions';
import { DEFAULT_LAYOUT } from './Constants';
import { InstanceDetailDrawer } from './index';

const CollectorsInstances = () => {
  const [selectedInstance, setSelectedInstance] = useState<CollectorInstanceView | null>(null);
  const { data: fleets } = useFleets();
  const { data: sources } = useSources();
  const defaultFilters = useDefaultInstanceFilters();

  const fleetNames = useMemo(
    () =>
      (fleets || []).reduce(
        (acc, fleet) => ({ ...acc, [fleet.id]: fleet.name }),
        {} as Record<string, string>,
      ),
    [fleets],
  );

  const columnRenderers = useMemo(
    () => customColumnRenderers({ fleetNames }),
    [fleetNames],
  );

  const fetchEntities = useCallback(
    (searchParams: SearchParams) => fetchPaginatedInstances(searchParams),
    [],
  );

  const getSourcesForInstance = (instance: CollectorInstanceView) =>
    (sources || []).filter((s) => s.fleet_id === instance.fleet_id);

  return (
    <>
      <PaginatedEntityTable<CollectorInstanceView>
        humanName="instances"
        entityActions={(instance: CollectorInstanceView) => <InstanceActions instance={instance} onDetailsClick={setSelectedInstance} />}
        tableLayout={DEFAULT_LAYOUT}
        fetchEntities={fetchEntities}
        keyFn={instancesKeyFn}
        entityAttributesAreCamelCase={false}
        columnRenderers={columnRenderers}
        defaultFilters={defaultFilters}
        bulkSelection={{ actions: <BulkActions /> }}
      />

      {selectedInstance && (
        <InstanceDetailDrawer
          instance={selectedInstance}
          sources={getSourcesForInstance(selectedInstance)}
          fleetName={fleetNames[selectedInstance.fleet_id] || selectedInstance.fleet_id}
          onClose={() => setSelectedInstance(null)}
        />
      )}
    </>
  );
};

export default CollectorsInstances;
