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

import { DocumentTitle, PageHeader } from 'components/common';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import type { SearchParams } from 'stores/PaginationTypes';

import { CollectorsPageNavigation } from 'components/collectors/common';
import { InstanceDetailDrawer } from 'components/collectors/instances';
import {
  fetchPaginatedInstances,
  instancesKeyFn,
  useFleets,
  useSources,
} from 'components/collectors/hooks';
import type { CollectorInstanceView } from 'components/collectors/types';
import customColumnRenderers from 'components/collectors/instances/ColumnRenderers';
import useTableElements from 'components/collectors/instances/useTableElements';
import { DEFAULT_LAYOUT, ADDITIONAL_ATTRIBUTES } from 'components/collectors/instances/Constants';

const CollectorsInstancesPage = () => {
  const [selectedInstance, setSelectedInstance] = useState<CollectorInstanceView | null>(null);
  const { data: fleets } = useFleets();
  const { data: sources } = useSources();

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

  const { entityActions } = useTableElements({
    onInstanceClick: setSelectedInstance,
  });

  const fetchEntities = useCallback(
    (searchParams: SearchParams) => fetchPaginatedInstances(searchParams),
    [],
  );

  const getSourcesForInstance = (instance: CollectorInstanceView) =>
    (sources || []).filter((s) => s.fleet_id === instance.fleet_id);

  return (
    <DocumentTitle title="Collector Instances">
      <CollectorsPageNavigation />
      <PageHeader title="Instances">
        <span>View all collector instances across fleets.</span>
      </PageHeader>

      <PaginatedEntityTable<CollectorInstanceView>
        humanName="instances"
        entityActions={entityActions}
        tableLayout={DEFAULT_LAYOUT}
        additionalAttributes={ADDITIONAL_ATTRIBUTES}
        fetchEntities={fetchEntities}
        keyFn={instancesKeyFn}
        entityAttributesAreCamelCase={false}
        columnRenderers={columnRenderers}
      />

      {selectedInstance && (
        <InstanceDetailDrawer
          instance={selectedInstance}
          sources={getSourcesForInstance(selectedInstance)}
          fleetName={fleetNames[selectedInstance.fleet_id] || selectedInstance.fleet_id}
          onClose={() => setSelectedInstance(null)}
        />
      )}
    </DocumentTitle>
  );
};

export default CollectorsInstancesPage;
