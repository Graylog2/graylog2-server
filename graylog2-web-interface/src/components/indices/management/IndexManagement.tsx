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
import { useState } from 'react';

import { Button, ButtonToolbar } from 'components/bootstrap';
import useUrlQueryFilters from 'components/common/EntityFilters/hooks/useUrlQueryFilters';
import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';

import AllocationPanel from './allocation/AllocationPanel';
import RetryAllocationConfirm from './allocation/RetryAllocationConfirm';
import { useRetryFailedAllocations } from './allocation/useAllocation';
import IndicesTable from './IndicesTable';
import useIndexOverview from './useIndexOverview';

const PICKED = 'index';

// The index list, with the shard allocation panel above it. Indices picked in the panel are the list's "index"
// filter, so they show as filter chips and add up to a selection for bulk actions.
const IndexManagement = () => {
  const currentUser = useCurrentUser();
  const { indices } = useIndexOverview();
  const [filters, setFilters] = useUrlQueryFilters();
  const { retryFailed, isRetrying } = useRetryFailedAllocations();
  const [showAllocation, setShowAllocation] = useState(false);
  const [confirmRetry, setConfirmRetry] = useState(false);

  const canExplain = isPermitted(currentUser.permissions, 'indexercluster:read');
  const canRetry = isPermitted(currentUser.permissions, 'indices:changestate');
  const picked = new Set(filters.get(PICKED) ?? []);

  const togglePicked = (name: string) => {
    const next = picked.has(name) ? [...picked].filter((value) => value !== name) : [...picked, name];
    setFilters(next.length > 0 ? filters.set(PICKED, next) : filters.remove(PICKED));
  };

  const confirmRetryFailed = async () => {
    try {
      await retryFailed();
    } finally {
      setConfirmRetry(false);
    }
  };

  const toolbar = (
    <ButtonToolbar>
      {canExplain && (
        <Button bsSize="small" onClick={() => setShowAllocation(true)} disabled={showAllocation}>
          Explain allocation
        </Button>
      )}
      {canRetry && (
        <Button bsSize="small" onClick={() => setConfirmRetry(true)}>
          Retry failed allocations
        </Button>
      )}
    </ButtonToolbar>
  );

  return (
    <>
      {showAllocation && (
        <AllocationPanel
          onClose={() => setShowAllocation(false)}
          picked={picked}
          onTogglePicked={togglePicked}
          onRetry={canRetry ? () => setConfirmRetry(true) : undefined}
          shardCounts={Object.fromEntries(indices.map((index) => [index.index, index.primary_shards]))}
        />
      )}
      <IndicesTable waitForNarrowing={showAllocation} topRightCol={toolbar} />
      {confirmRetry && (
        <RetryAllocationConfirm
          isRetrying={isRetrying}
          onConfirm={confirmRetryFailed}
          onCancel={() => setConfirmRetry(false)}
        />
      )}
    </>
  );
};

export default IndexManagement;
