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
import type { IndexInfo } from 'stores/indices/IndicesStore';

type MultipleIndices = Array<IndexInfo>;

const getMultiple = async (indices: Array<string>): Promise<MultipleIndices> => IndexerIndices.multiple({ indices });

const INDEXER_INDICES_MULTIPLE_QUERY_KEY = 'indexer_indices_multiple';
export const getIndexerIndicesQueryKey = (indices: Array<string>) => [INDEXER_INDICES_MULTIPLE_QUERY_KEY, indices];

const useIndexerIndicesMultiple = (
  indices: Array<string>,
  { enabled } = { enabled: true },
): {
  data: MultipleIndices;
  isLoading: boolean;
  refetch: () => void;
} => {
  const { data, isLoading, refetch } = useQuery({
    queryKey: getIndexerIndicesQueryKey(indices),
    queryFn: () =>
      defaultOnError(
        getMultiple(indices),
        `Loading Indexer multiple indices failed with status`,
        `Could not load Indexer multiple indices `,
      ),
    enabled,
  });

  return {
    data,
    isLoading,
    refetch,
  };
};

export default useIndexerIndicesMultiple;
