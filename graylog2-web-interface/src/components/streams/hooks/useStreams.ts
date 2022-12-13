import { useQuery } from '@tanstack/react-query';

import UserNotification from 'util/UserNotification';
import type { SearchParams } from 'stores/PaginationTypes';
import type { Stream } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';

const useStreams = (searchParams: SearchParams): { data: { streams: Array<Stream>, pagination: { total: number } } | undefined, refetch: () => void } => {
  const { data, refetch } = useQuery(
    ['streams', 'overview', searchParams],
    () => StreamsStore.searchPaginated(
      searchParams.page,
      searchParams.pageSize,
      searchParams.query,
      { sort: searchParams?.sort.attributeId, direction: searchParams?.sort.direction },
    ),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading streams failed with status: ${errorThrown}`,
          'Could not load streams');
      },
      keepPreviousData: true,
    },
  );

  return ({
    data,
    refetch,
  });
};

export default useStreams;
