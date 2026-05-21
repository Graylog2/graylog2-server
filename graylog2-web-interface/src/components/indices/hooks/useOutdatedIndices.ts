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

import { defaultOnError } from 'util/conditional/onError';

export type OutdatedIndex = {
  index_name: string;
  version: string;
  warm_index: boolean;
  managed_index: boolean;
  system_index: boolean;
};

// TODO: REMOVE — local UI mock for OutdatedIndicesTable. Delete this block to restore real data.
const USE_MOCK_OUTDATED_INDICES = true;
const MOCK_VERSIONS = ['7.10.2', '6.8.23', '1.3.18'];
const mockVersion = (i: number) => MOCK_VERSIONS[i % MOCK_VERSIONS.length];
const MOCK_OUTDATED_INDICES: Array<OutdatedIndex> = [
  ...Array.from({ length: 20 }, (_, i) => ({
    index_name: `graylog_${i}`,
    version: mockVersion(i),
    warm_index: i % 4 === 0,
    managed_index: true,
    system_index: false,
  })),
  ...Array.from({ length: 21 }, (_, i) => ({
    index_name: `.system_index_${i}`,
    version: mockVersion(i),
    warm_index: false,
    managed_index: false,
    system_index: true,
  })),
  ...Array.from({ length: 22 }, (_, i) => ({
    index_name: `legacy_unknown_${i}`,
    version: mockVersion(i),
    warm_index: false,
    managed_index: false,
    system_index: false,
  })),
];

const useOutdatedIndices = () => {
  const {
    data = [],
    isError,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['outdatedIndices'],
    queryFn: () =>
      defaultOnError(
        IndexerIndices.getOutdatedIndices() as Promise<Array<OutdatedIndex>>,
        'Loading outdated indices failed',
        'Could not load outdated indices',
      ),
    retry: false,
    enabled: !USE_MOCK_OUTDATED_INDICES,
  });

  if (USE_MOCK_OUTDATED_INDICES) {
    return {
      data: MOCK_OUTDATED_INDICES,
      isError: false,
      isLoading: false,
      refetch,
    };
  }

  return {
    data,
    isError,
    isLoading,
    refetch,
  };
};

export default useOutdatedIndices;
