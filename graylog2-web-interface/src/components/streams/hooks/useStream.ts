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

import type { Stream } from 'stores/streams/StreamsStore';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { defaultOnError } from 'util/conditional/onError';

const fetchStream = (streamId: string) => {
  const { url } = ApiRoutes.StreamsApiController.get(streamId);

  return fetch('GET', qualifyUrl(url));
};

const useStream = (streamId: string, { enabled } = { enabled: true }): {
  data: Stream
  refetch: () => void,
  isFetching: boolean,
  isError,
} => {
  const { data, refetch, isFetching, isError } = useQuery(
    ['stream', streamId],
    () => defaultOnError(fetchStream(streamId), 'Loading stream failed with status', 'Could not load Stream'),
    {
      keepPreviousData: true,
      enabled,
    },
  );

  return ({
    data,
    refetch,
    isFetching,
    isError,
  });
};

export default useStream;
