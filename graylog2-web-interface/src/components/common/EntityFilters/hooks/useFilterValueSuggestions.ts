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

import PaginationURL from 'util/PaginationURL';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { defaultOnError } from 'util/conditional/onError';

const DEFAULT_DATA = {
  pagination: {
    total: 0,
  },
  suggestions: [],
};

type SearchParams = {
  page: number,
  pageSize: number,
  query: string,
}

type PaginatedSuggestions = {
  pagination: { total: number }
  suggestions: Array<{ id: string, value: string }>,
}

const fetchFilterValueSuggestions = async (collection: string, { query, page, pageSize }: SearchParams, collectionProperty: string = 'title'): Promise<PaginatedSuggestions | undefined> => {
  const additional = {
    collection,
    column: collectionProperty,
  };
  const url = PaginationURL('entity_suggestions', page, pageSize, query, additional);

  return fetch('GET', qualifyUrl(url));
};

const useFilterValueSuggestions = (
  attributeId: string,
  collection: string,
  searchParams: SearchParams,
  collectionProperty: string,
): {
  data: PaginatedSuggestions | undefined
  isInitialLoading: boolean
} => {
  if (!collection) {
    throw Error(`Attribute meta data for attribute "${attributeId}" is missing related collection.`);
  }

  const { data, isInitialLoading } = useQuery(
    ['filters', 'suggestions', searchParams],
    () => defaultOnError(fetchFilterValueSuggestions(collection, searchParams, collectionProperty), 'Loading suggestions for filter failed with status', 'Could not load filter suggestions'),
    {
      retry: 0,
      keepPreviousData: true,
    });

  return {
    data: data ?? DEFAULT_DATA,
    isInitialLoading,
  };
};

export default useFilterValueSuggestions;
