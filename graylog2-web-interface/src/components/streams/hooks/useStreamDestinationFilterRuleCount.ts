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
import { create, windowScheduler, indexedResolver } from '@yornaath/batshit';

import type FetchError from 'logic/errors/FetchError';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

const streamDestinationFilterRuleCounts = create({
  fetcher: (streamIds: Array<string>) =>
    fetch('POST', qualifyUrl(ApiRoutes.StreamOutputFilterRuleApiController.countByStreams().url), {
      stream_ids: streamIds,
    }),
  resolver: indexedResolver(),
  scheduler: windowScheduler(10),
});

const useStreamDestinationFilterRuleCount = (
  streamId: string,
  enabled: boolean = true,
): {
  data: number;
  refetch: () => void;
  isInitialLoading: boolean;
  error: FetchError;
  isError: boolean;
} => {
  const { data, refetch, isInitialLoading, error, isError } = useQuery<number, FetchError>({
    queryKey: ['stream', 'destination-filters', 'count', streamId],
    queryFn: () => streamDestinationFilterRuleCounts.fetch(streamId),
    notifyOnChangeProps: ['data', 'error'],
    enabled,
  });

  return {
    data: data ?? 0,
    refetch,
    isInitialLoading,
    error,
    isError,
  };
};

export default useStreamDestinationFilterRuleCount;
