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

import useUrlQueryFilters from 'components/common/EntityFilters/hooks/useUrlQueryFilters';
import usePermissions from 'hooks/usePermissions';

import AllocationPanel from './allocation/AllocationPanel';
import IndicesTable from './IndicesTable';
import useAllocationPanel from './useAllocationPanel';
import useIndexOverview from './useIndexOverview';
import useRetryAllocationsConfirm from './useRetryAllocationsConfirm';

const PICKED = 'index';

// The index list, with the shard allocation panel above it (opened from the page header's AllocationActions). Indices picked in the panel are the list's "index"
// filter, so they show as filter chips and add up to a selection for bulk actions.
const IndexManagement = () => {
  const { isPermitted } = usePermissions();
  const { indices } = useIndexOverview();
  const [filters, setFilters] = useUrlQueryFilters();
  const [allocationRequested, setShowAllocation] = useAllocationPanel();
  const { askToRetry, confirmDialog } = useRetryAllocationsConfirm();

  const canExplain = isPermitted('indexercluster:read');
  const canRetry = isPermitted('indices:changestate');
  const showAllocation = canExplain && allocationRequested;
  const picked = new Set(filters.get(PICKED) ?? []);

  const togglePicked = (name: string) => {
    const next = picked.has(name) ? [...picked].filter((value) => value !== name) : [...picked, name];
    setFilters(next.length > 0 ? filters.set(PICKED, next) : filters.remove(PICKED));
  };

  return (
    <>
      {showAllocation && (
        <AllocationPanel
          onClose={() => setShowAllocation(false)}
          picked={picked}
          onTogglePicked={togglePicked}
          onRetry={canRetry ? askToRetry : undefined}
          shardCounts={Object.fromEntries(indices.map((index) => [index.index, index.primary_shards]))}
        />
      )}
      <IndicesTable waitForNarrowing={showAllocation} />
      {confirmDialog}
    </>
  );
};

export default IndexManagement;
