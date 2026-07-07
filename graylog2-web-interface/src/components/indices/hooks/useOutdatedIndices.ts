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
import { useQuery } from '@tanstack/react-query';

import { IndexerIndices } from '@graylog/server-api';

export type OutdatedIndex = {
  index_name: string;
  version: string;
  warm_index: boolean;
  managed_index: boolean;
  system_index: boolean;
  /** Id of the index set this index is the active write index of, `null` for all other indices. */
  active_write_index: string | null;
};

// While errored, retry gently in the background so transient failures heal without a reload.
const ERROR_REFETCH_INTERVAL_MS = 30000;

const useOutdatedIndices = () => {
  const {
    data = [],
    isError,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['outdatedIndices'],
    // No error toast: the panels render a persistent error state, and background retries would spam toasts.
    queryFn: () => IndexerIndices.getOutdatedIndices() as Promise<Array<OutdatedIndex>>,
    retry: 2,
    refetchInterval: (query) => (query.state.status === 'error' ? ERROR_REFETCH_INTERVAL_MS : false),
  });

  return {
    data,
    isError,
    isLoading,
    refetch,
  };
};

export default useOutdatedIndices;
