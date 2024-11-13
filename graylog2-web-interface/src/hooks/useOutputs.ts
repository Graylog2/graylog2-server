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

import type { Output } from 'stores/outputs/OutputsStore';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { defaultOnError } from 'util/conditional/onError';

type OutputRequestResponse = {
  outputs: Array<Output>,
    total: number,
}
export const KEY_PREFIX = ['outputs', 'overview'];
export const keyFn = () => [...KEY_PREFIX];

export const fetchOutputs = () => {
  const url = qualifyUrl(ApiRoutes.OutputsApiController.index().url);

  return fetch('GET', url);
};

type Options = {
  enabled: boolean,
}

const useOutputs = ({ enabled }: Options = { enabled: true }): {
  data: OutputRequestResponse,
  refetch: () => void,
  isInitialLoading: boolean,
} => {
  const { data, refetch, isInitialLoading } = useQuery(
    keyFn(),
    () => defaultOnError(fetchOutputs(), 'Loading outputs failed with status', 'Could not load outputs'),
    {
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

export default useOutputs;
