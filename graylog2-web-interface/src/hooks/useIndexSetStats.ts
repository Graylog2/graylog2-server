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

import { SystemIndexSets } from '@graylog/server-api';

import type { IndexSetStats } from 'stores/indices/IndexSetsStore';
import { defaultOnError } from 'util/conditional/onError';
import i18n from 'i18n';

const fetchIndexSetStats = (indexSetId: string): Promise<IndexSetStats> =>
  SystemIndexSets.indexSetStatistics(indexSetId);

const useIndexSetStats = (indexSetId: string) => {
  const { data, refetch, isLoading, error, isSuccess } = useQuery({
    queryKey: ['indexSet', indexSetId, 'stats'],

    queryFn: () =>
      defaultOnError(
        fetchIndexSetStats(indexSetId),
        i18n.t('use_index_set_stats_notifications.load_failed', { ns: 'indexsets' }),
        i18n.t('use_index_set_stats_notifications.load_failed_title', { ns: 'indexsets' }),
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

export default useIndexSetStats;
