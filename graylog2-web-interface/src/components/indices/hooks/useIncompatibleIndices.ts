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

import { SystemIndexerIndices } from '@graylog/server-api';

export type IncompatibleIndex = {
  index_name: string;
  version: string;
  warm_index: boolean;
  managed_index: boolean;
  system_index: boolean;
  active_write_index: string | null;
  begin?: string | null;
  end?: string | null;
};

const ERROR_REFETCH_INTERVAL_MS = 30000;

const useIncompatibleIndices = () => {
  const {
    data = [],
    isError,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['incompatibleIndices'],
    queryFn: () => SystemIndexerIndices.getOutdatedIndices() as Promise<Array<IncompatibleIndex>>,
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

export default useIncompatibleIndices;
