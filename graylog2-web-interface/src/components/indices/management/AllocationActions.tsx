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

import { Button, ButtonToolbar } from 'components/bootstrap';
import usePermissions from 'hooks/usePermissions';

import { useExplainAgain } from './allocation/useAllocation';
import useAllocationPanel from './useAllocationPanel';
import useRetryAllocationsConfirm from './useRetryAllocationsConfirm';

// The page header's actions, styled as the Index Sets page's: open the shard allocation panel, retry failed allocations.
const AllocationActions = () => {
  const { isPermitted } = usePermissions();
  const [allocationRequested, setShowAllocation] = useAllocationPanel();
  const { askToRetry, confirmDialog } = useRetryAllocationsConfirm();
  const { explainAgain, isExplaining } = useExplainAgain();
  const canExplain = isPermitted('indexercluster:read');
  const canRetry = isPermitted('indices:changestate');

  if (!canExplain && !canRetry) {
    return null;
  }

  return (
    <ButtonToolbar>
      {canExplain && (
        // Opens the panel; once open, asks OpenSearch again. Busy only while waiting for an answer.
        <Button
          bsStyle="primary"
          onClick={() => (allocationRequested ? explainAgain() : setShowAllocation(true))}
          disabled={isExplaining}>
          {isExplaining ? 'Explaining...' : 'Explain allocation'}
        </Button>
      )}
      {canRetry && (
        <Button bsStyle="info" onClick={askToRetry}>
          Retry failed allocations
        </Button>
      )}
      {confirmDialog}
    </ButtonToolbar>
  );
};

export default AllocationActions;
