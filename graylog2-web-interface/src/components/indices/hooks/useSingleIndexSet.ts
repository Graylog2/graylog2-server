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

import fetch from 'logic/rest/FetchProvider';
import type FetchError from 'logic/errors/FetchError';
import { type IndexSet } from 'stores/indices/IndexSetsStore';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { defaultOnError } from 'util/conditional/onError';

const fetchIndexSet = (indexSetId: string) => fetch('GET', qualifyUrl(ApiRoutes.IndexSetsApiController.get(indexSetId).url));

const useSingleIndexSet = (indexSetId: string) : {
  data: IndexSet,
  refetch: () => void,
  isSuccess: boolean,
  isInitialLoading: boolean,
} => {
  const { data, refetch, isInitialLoading, isSuccess } = useQuery<IndexSet, FetchError>(
    ['indexSet', indexSetId],
    () => defaultOnError(fetchIndexSet(indexSetId), `Loading index set with id: ${indexSetId} failed with status`, 'Could not load index set'),
    {
      keepPreviousData: true,
    },
  );

  return ({
    data,
    refetch,
    isSuccess,
    isInitialLoading,
  });
};

export default useSingleIndexSet;
