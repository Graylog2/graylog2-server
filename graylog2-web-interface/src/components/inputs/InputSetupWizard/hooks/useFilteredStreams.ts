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

import type FetchError from 'logic/errors/FetchError';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import { onError } from 'util/conditional/onError';
import type { Stream } from 'stores/streams/StreamsStore';
import UserNotification from 'util/UserNotification';

const INITIAL_DATA = {
  total: 0,
  streams: [],
};

type StreamsResponse = {
  total: number;
  streams: Array<Stream>;
};

export const STREAMS_NO_SECURITY_ID = ['streams_without_security', 'overview'];

const getStreams = (): Promise<StreamsResponse> =>
  fetch('GET', qualifyUrl(ApiRoutes.StreamsApiController.paginatedWithoutSecurityDefaults().url));

const useStreams = (): {
  data: StreamsResponse;
  isLoading: boolean;
} => {
  const { data, isLoading } = useQuery<StreamsResponse, FetchError>([STREAMS_NO_SECURITY_ID], () =>
    onError(getStreams(), (errorThrown: FetchError) => {
      if (!(errorThrown.status === 404)) {
        UserNotification.error(`Loading streams failed with: ${errorThrown}`);
      }
    }),
  );

  return {
    data: data ?? INITIAL_DATA,
    isLoading,
  };
};

export default useStreams;
