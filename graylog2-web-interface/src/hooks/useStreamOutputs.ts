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

import { StreamOutputs } from '@graylog/server-api';
import UserNotification from 'util/UserNotification';
import type { Output } from 'stores/outputs/OutputsStore';

export const KEY_PREFIX = ['outputs', 'overview'];
export const keyFn = (streamId: string) => [...KEY_PREFIX, streamId];

export const fetchStreamOutputs = (streamId: string) => StreamOutputs.get(streamId);
type Options = {
  enabled: boolean,
}

const useStreamOutputs = (streamId: string, { enabled }: Options = { enabled: true }): {
  data: {
    outputs: Array<Output>,
    total: number,
  }
  refetch: () => void,
  isInitialLoading: boolean,
} => {
  const { data, refetch, isInitialLoading } = useQuery(
    keyFn(streamId),
    () => fetchStreamOutputs(streamId),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading stream outputs failed with status: ${errorThrown}`,
          'Could not load stream outputs');
      },
      keepPreviousData: true,
      enabled,
    },
  );

  return ({
    data,
    refetch,
    isInitialLoading,
  });
};

export default useStreamOutputs;
