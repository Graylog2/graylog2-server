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
import { useCallback } from 'react';
import { useQuery, keepPreviousData } from '@tanstack/react-query';

import { QuickJump } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';
import { getEntityRoute, usePluginEntityTypeGenerators } from 'routing/hooks/useShowRouteForEntity';
import usePluginEntities from 'hooks/usePluginEntities';
import useNavItems from 'components/quick-jump/hooks/useNavItems';
import { PAGE_WEIGHT, BASE_SCORE } from 'components/quick-jump/Constants';
import type { SearchResultItem } from 'components/quick-jump/Types';

const compareSearchItems = (result1: SearchResultItem, result2: SearchResultItem) => {
  const scoreDifference = result2.score - result1.score;
  if (scoreDifference === 0) {
    return Number(result2.last_opened ?? false) - Number(result1.last_opened ?? false);
  }

  return scoreDifference;
};

const normalize = (s: string) => s.toLocaleLowerCase().trim();

const scoreItem = (item: { title: string }, query: string) => {
  const normalizedTitle = normalize(item.title);
  const normalizedQuery = normalize(query);
  if (normalizedTitle === normalizedQuery) {
    return BASE_SCORE;
  }
  if (normalizedTitle.startsWith(query)) {
    return BASE_SCORE - 1;
  }
  if (normalizedTitle.includes(normalizedQuery)) {
    return BASE_SCORE - 2;
  }

  return 0;
};

const scoreResults = (items: Array<SearchResultItem>, query: string, weight = 1.0) =>
  items.flatMap((item) => {
    const score = scoreItem(item, query);
    if (score === 0) {
      return [];
    }

    return [{ ...item, score: score * weight }];
  });

type ItemResultType = Awaited<ReturnType<typeof QuickJump.search>>['results'][number];

const useEntityResultMapper = () => {
  const pluginEntityRoutesResolver = usePluginEntities('entityRoutes');
  const entityTypeGenerators = usePluginEntityTypeGenerators();

  return useCallback(
    (item: ItemResultType): SearchResultItem => ({
      ...item,
      key: item.id,
      link: getEntityRoute(item.id, item.type, pluginEntityRoutesResolver, entityTypeGenerators),
    }),
    [entityTypeGenerators, pluginEntityRoutesResolver],
  );
};

const useQuickJumpSearch = (searchQuery: string) => {
  const mapEntityResult = useEntityResultMapper();
  const navItems = useNavItems();

  const { data: searchResultItems, isLoading } = useQuery({
    queryKey: ['quick-jump', searchQuery],
    queryFn: () =>
      defaultOnError(
        (async () => {
          const entities = await QuickJump.search({ query: searchQuery, limit: 100 });
          const scoredNavItems: SearchResultItem[] = scoreResults(navItems, searchQuery, PAGE_WEIGHT);
          const entityResultItems = entities.results.map(mapEntityResult);

          return (searchQuery.trim() !== '' ? [...entityResultItems, ...scoredNavItems] : [...entityResultItems]).sort(
            compareSearchItems,
          );
        })(),
        'Fetch Entities Search Results failed with status',
        'Could not Fetch Entity Search Results.',
      ),
    placeholderData: keepPreviousData,
    refetchOnMount: 'always',
  });

  return {
    searchResults: searchResultItems ?? [],
    isLoading,
  };
};

export default useQuickJumpSearch;
