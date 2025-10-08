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

import { QuickJump } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';
import { getEntityRoute, usePluginEntityTypeGenerators } from 'routing/hooks/useShowRouteForEntity';
import usePluginEntities from 'hooks/usePluginEntities';
import useDebouncedValue from 'hooks/useDebouncedValue';

import type { SearchResultItem } from '../Types';

export type QuickJumpRequest = {
  query: string;
  limit?: number;
};
const useEntitySearchResults = (request: QuickJumpRequest) => {
  const pluginEntityRoutesResolver = usePluginEntities('entityRoutes');
  const entityTypeGenerators = usePluginEntityTypeGenerators();
  const [searchQuery] = useDebouncedValue(request?.query, 500);

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
  }));

  return entitiesSearchResults
    ? {
        data: searchResultItems,
        isLoading,
      }
    : { isLoading };
};

export default useEntitySearchResults;
