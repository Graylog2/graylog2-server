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
