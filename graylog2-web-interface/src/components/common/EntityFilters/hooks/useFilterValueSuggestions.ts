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

import UserNotification from 'util/UserNotification';
import PaginationURL from 'util/PaginationURL';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import {Attribute} from 'stores/PaginationTypes';

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

const fetchFilterValueSuggestions = async (collection: string, { query, page, pageSize }: SearchParams, staticEntries: string, collectionProperty: string = 'title'): Promise<PaginatedSuggestions | undefined> => {
  const additional = {
    collection,
    column: collectionProperty,
  };
  let url = PaginationURL('entity_suggestions', page, pageSize, query, additional);

  if (staticEntries) {
    url = `${url}&staticEntries=${staticEntries}`;
  }

  return fetch('GET', qualifyUrl(url));
};

const useFilterValueSuggestions = (
  attribute: Attribute,
  searchParams: SearchParams,
): {
  data: PaginatedSuggestions | undefined
  isInitialLoading: boolean
} => {
  if (!attribute.related_collection) {
    throw Error(`Attribute meta data for attribute "${attribute.id}" is missing related collection.`);
  }

  console.log({ attribute });
  const staticEntries = attribute.filter_options?.filter((value) => value.title === 'static').map((value) => value.value).join(',');

  const { data, isInitialLoading } = useQuery(['filters', 'suggestions', searchParams], () => fetchFilterValueSuggestions(attribute.related_collection, searchParams, staticEntries, attribute.related_property), {
    onError: (errorThrown) => {
      UserNotification.error(`Loading suggestions for filter failed with status: ${errorThrown}`,
        'Could not load filter suggestions');
    },
    retry: 0,
    keepPreviousData: true,
  });

  return {
    data: data ?? DEFAULT_DATA,
    isInitialLoading,
  };
};

export default useFilterValueSuggestions;
