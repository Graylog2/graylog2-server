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
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import type { IndexerOverview } from 'stores/indexers/IndexerOverviewStore';
import { defaultOnError } from 'util/conditional/onError';

const fetchIndexerOverview = (indexSetId: string): Promise<IndexerOverview> =>
  fetch('GET', qualifyUrl(ApiRoutes.IndexerOverviewApiResource.list(indexSetId).url));

const useIndexerOverview = (indexSetId: string) => {
  const { data, refetch, isLoading, error, isSuccess } = useQuery({
    queryKey: ['indexerOverview', indexSetId, 'stats'],
    queryFn: () =>
      defaultOnError(
        fetchIndexerOverview(indexSetId),
        `Loading indexer overview for index set failed with status`,
        'Could not load indexer overview.',
      ),

    notifyOnChangeProps: ['data', 'error'],
  });

  return {
    data,
    refetch,
    isLoading,
    error,
    isSuccess,
  };
};

export default useIndexerOverview;
