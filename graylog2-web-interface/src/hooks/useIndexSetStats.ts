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
import { type IndexSetStats } from 'stores/indices/IndexSetsStore';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { defaultOnError } from 'util/conditional/onError';

const fetchIndexSetStats = (indexSetId: string) => fetch('GET', qualifyUrl(ApiRoutes.IndexSetsApiController.getIndexSetStats(indexSetId).url));

const useIndexSetStats = (indexSetId: string): {
  data: IndexSetStats,
  refetch: () => void,
  isLoading: boolean,
  error: FetchError,
  isSuccess: boolean,
} => {
  const { data, refetch, isLoading, error, isSuccess } = useQuery<IndexSetStats, FetchError>(
    ['indexSet', indexSetId, 'stats'],
    () => defaultOnError(fetchIndexSetStats(indexSetId), 'Loading index set stats failed with status', 'Could not load index set stats.'),
    {
      notifyOnChangeProps: ['data', 'error'],
    },
  );

  return ({
    data,
    refetch,
    isLoading,
    error,
    isSuccess,
  });
};

export default useIndexSetStats;
