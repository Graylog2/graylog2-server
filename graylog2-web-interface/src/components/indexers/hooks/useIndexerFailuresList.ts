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

import { IndexerFailures } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

const INDEXER_FAILURES_LIST_QUERY_KEY = 'indexer_failures_list';

const useIndexerFailuresList = (
  limit: number,
  offset: number,
): {
  data: unknown;
  isLoading: boolean;
} => {
  const { data, isLoading } = useQuery({
    queryKey: [INDEXER_FAILURES_LIST_QUERY_KEY, limit, offset],
    queryFn: () =>
      defaultOnError(
        IndexerFailures.single(limit, offset),
        'Loading indexer failures list failed with status',
        'Could not load indexer failures list',
      ),
  });

  return {
    data,
    isLoading,
  };
};

export default useIndexerFailuresList;
