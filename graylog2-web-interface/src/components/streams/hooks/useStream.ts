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

import UserNotification from 'util/UserNotification';
import type { Stream } from 'stores/streams/StreamsStore';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';

const fetchStream = (streamId: string) => {
  const { url } = ApiRoutes.StreamsApiController.get(streamId);

  return fetch('GET', qualifyUrl(url));
};

const useStream = (streamId: string): {
  data: Stream
  refetch: () => void,
  isFetching: boolean,
} => {
  const { data, refetch, isFetching } = useQuery(
    ['streams', streamId],
    () => fetchStream(streamId),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading stream failed with status: ${errorThrown}`,
          'Could not load Stream');
      },
      keepPreviousData: true,
    },
  );

  return ({
    data,
    refetch,
    isFetching,
  });
};

export default useStream;
