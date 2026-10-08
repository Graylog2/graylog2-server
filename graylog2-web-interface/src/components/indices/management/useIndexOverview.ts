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

import { IndexerIndicesManagement } from '@graylog/server-api';

import type { IndexSummary } from './types';

export const INDEX_OVERVIEW_QUERY_KEY = ['indices', 'management', 'overview'];

// Every index, for what the paged table doesn't hold: the selection's rows and the shard counts of the allocation
// panel. The table refreshes itself; this one follows when an action invalidates it.
const useIndexOverview = (): { indices: Array<IndexSummary>; isLoading: boolean } => {
  const { data, isLoading } = useQuery({
    queryKey: INDEX_OVERVIEW_QUERY_KEY,
    queryFn: () => IndexerIndicesManagement.list(),
  });

  return { indices: data?.indices ?? [], isLoading };
};

export default useIndexOverview;
