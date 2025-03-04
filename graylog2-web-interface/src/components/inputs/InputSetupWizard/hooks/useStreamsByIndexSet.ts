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
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import type { Stream } from 'logic/streams/types';
import UserNotification from 'util/UserNotification';
import { onError } from 'util/conditional/onError';

export const STREAMS_BY_INDEX_SET_ID = 'streams_by_index_set_id';

type ResponseData = {
  total: number,
  streams: Array<Stream>
}

const fetchStreamsByIndexSet = async (indexSetId: string): Promise<ResponseData> => {
  const url = qualifyUrl(ApiRoutes.StreamsApiController.byIndexSet(indexSetId).url);

  return fetch('GET', url);
};

const useStreamsByIndexSet = (indexSetId: string, enabled: boolean = true): {
  data: ResponseData,
  isLoading: boolean,
} => {
  const { data, isLoading } = useQuery<ResponseData, FetchError>(
    [STREAMS_BY_INDEX_SET_ID],
    () => onError(fetchStreamsByIndexSet(indexSetId), (errorThrown: FetchError) => {
      if (!(errorThrown.status === 404)) {
        UserNotification.error(`Loading streams by index set failed with: ${errorThrown}`);
      }
    }),
    { enabled });

  return ({
    data: data,
    isLoading,
  });
};

export default useStreamsByIndexSet;
