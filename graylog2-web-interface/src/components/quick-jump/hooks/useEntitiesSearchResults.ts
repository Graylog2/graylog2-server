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
import { defaultOnError } from 'util/conditional/onError';
import { getEntityRoute } from 'routing/hooks/useShowRouteForEntity';

import type { SearchResultItem } from '../Types';
import { ENTITY_TYPE } from '../Constants';

export type QuickJumpRequest = {
  query: string;
  limit?: number;
}
export type QuickJumpResponse = {
  id: string;
  type: string;
  title: string;
}

export const fetchEntitiesSearchResults = (request: QuickJumpRequest) => fetch<{ results: QuickJumpResponse[] }>('POST', qualifyUrl('quickjump'), request, false);

const useEntitiesSearchResults = (request: QuickJumpRequest): SearchResultItem[] => {
  const { data: entitiesSearchResults } = useQuery({
    queryKey: ['quick-jump', request],
    queryFn: () =>
      defaultOnError(
        fetchEntitiesSearchResults(request),
        'Fetch Entities Search Results failed with status',
        'Could not Fetch Entities Search Results.',
      ),
    enabled: !!request?.query,
  });

  const searchResultItems: SearchResultItem[]  = entitiesSearchResults?.results?.map((item) => ({
    type: ENTITY_TYPE,
    title: `${item.type} / ${item.title}`,
    link: getEntityRoute(item.id, item.type),
    backendScore: 100,
  }))
  
  return searchResultItems || [];
};

export default useEntitiesSearchResults;