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
import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';

import { QuickJump } from '@graylog/server-api';

import useLastOpened from 'components/welcome/hooks/useLastOpened';
import { defaultOnError } from 'util/conditional/onError';
import { getEntityRoute, usePluginEntityTypeGenerators } from 'routing/hooks/useShowRouteForEntity';
import usePluginEntities from 'hooks/usePluginEntities';
import useDebouncedValue from 'hooks/useDebouncedValue';
import { createGRN } from 'logic/permissions/GRN';
import { LAST_OPENED_ITEMS_LOOKBACK } from 'components/quick-jump/Constants';

import type { SearchResultItem } from '../Types';

export type QuickJumpRequest = {
  query: string;
  limit?: number;
};
const useEntitySearchResults = (request: QuickJumpRequest) => {
  const lastOpened = useLastOpened({ page: 1, per_page: LAST_OPENED_ITEMS_LOOKBACK });
  const lastOpenedGRNs = useMemo(
    () => lastOpened?.data?.lastOpened?.map((item) => item.grn) ?? [],
    [lastOpened?.data?.lastOpened],
  );

  const pluginEntityRoutesResolver = usePluginEntities('entityRoutes');
  const entityTypeGenerators = usePluginEntityTypeGenerators();
  const [searchQuery] = useDebouncedValue(request?.query, 300);

  const { data: entitiesSearchResults, isLoading } = useQuery({
    queryKey: ['quick-jump', request],
    queryFn: () =>
      defaultOnError(
        QuickJump.search({ ...request, limit: 100 }),
        'Fetch Entities Search Results failed with status',
        'Could not Fetch Entity Search Results.',
      ),
    enabled: !!searchQuery,
    refetchOnMount: 'always',
    staleTime: 0,
  });

  const searchResultItems: SearchResultItem[] = entitiesSearchResults?.results?.map((item) => ({
    key: item.id,
    type: item.type,
    title: item.title,
    link: getEntityRoute(item.id, item.type, pluginEntityRoutesResolver, entityTypeGenerators),
    score: item.score,
    lastOpened: lastOpenedGRNs.includes(createGRN(item.type, item.id)),
  }));

  return entitiesSearchResults
    ? {
        data: searchResultItems,
        isLoading,
      }
    : { isLoading };
};

export default useEntitySearchResults;
