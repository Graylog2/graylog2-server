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
import { useState, useMemo } from 'react';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { InstanceList } from 'components/collectors/instances';
import { useInstances, useFleets, useSources } from 'components/collectors/hooks';
import { CollectorsPageNavigation, FilterBar } from 'components/collectors/common';

type StatusFilter = 'all' | 'online' | 'offline';

const CollectorsInstancesPage = () => {
  const { data: instances, isLoading: instancesLoading } = useInstances();
  const { data: fleets, isLoading: fleetsLoading } = useFleets();
  const { data: sources, isLoading: sourcesLoading } = useSources();

  const [searchValue, setSearchValue] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');
  const [fleetFilter, setFleetFilter] = useState<string | null>(null);

  const isLoading = instancesLoading || fleetsLoading || sourcesLoading;

  const fleetNames = useMemo(
    () =>
      (fleets || []).reduce(
        (acc, fleet) => ({ ...acc, [fleet.id]: fleet.name }),
        {} as Record<string, string>,
      ),
    [fleets],
  );

  const fleetOptions = useMemo(
    () => (fleets || []).map((fleet) => ({ value: fleet.id, label: fleet.name })),
    [fleets],
  );

  const filteredInstances = useMemo(() => {
    let result = instances || [];

    if (searchValue) {
      const search = searchValue.toLowerCase();
      result = result.filter(
        (i) =>
          i.hostname?.toLowerCase().includes(search) ||
          i.agent_id.toLowerCase().includes(search),
      );
    }

    if (statusFilter !== 'all') {
      result = result.filter((i) => i.status === statusFilter);
    }

    if (fleetFilter) {
      result = result.filter((i) => i.fleet_id === fleetFilter);
    }

    return result;
  }, [instances, searchValue, statusFilter, fleetFilter]);

  return (
    <DocumentTitle title="Collector Instances">
      <CollectorsPageNavigation />
      <PageHeader title="Instances">
        <span>View all collector instances across fleets.</span>
      </PageHeader>
      {isLoading ? (
        <Spinner />
      ) : (
        <>
          <FilterBar
            searchValue={searchValue}
            onSearchChange={setSearchValue}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            fleetFilter={fleetFilter}
            onFleetFilterChange={setFleetFilter}
            fleetOptions={fleetOptions}
          />
          <InstanceList
            instances={filteredInstances}
            fleetNames={fleetNames}
            sources={sources || []}
          />
        </>
      )}
    </DocumentTitle>
  );
};

export default CollectorsInstancesPage;
