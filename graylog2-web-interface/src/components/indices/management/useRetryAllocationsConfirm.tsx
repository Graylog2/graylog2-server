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

import RetryAllocationConfirm from './allocation/RetryAllocationConfirm';
import { useRetryFailedAllocations } from './allocation/useAllocation';

// Asks before retrying failed allocations: `askToRetry` opens the confirmation, `confirmDialog` renders it.
const useRetryAllocationsConfirm = () => {
  const { retryFailed, isRetrying } = useRetryFailedAllocations();
  const [confirming, setConfirming] = useState(false);

  const onConfirm = async () => {
    try {
      await retryFailed();
    } finally {
      setConfirming(false);
    }
  };

  const confirmDialog = confirming ? (
    <RetryAllocationConfirm isRetrying={isRetrying} onConfirm={onConfirm} onCancel={() => setConfirming(false)} />
  ) : null;

  return { askToRetry: () => setConfirming(true), confirmDialog };
};

export default useRetryAllocationsConfirm;
