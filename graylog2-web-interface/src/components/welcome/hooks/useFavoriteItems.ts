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

import type { PaginatedFavoriteItems, RequestQuery } from 'components/welcome/types';
import { DEFAULT_PAGINATION } from 'components/welcome/Constants';
import PaginationURL from 'util/PaginationURL';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { defaultOnError } from 'util/conditional/onError';

export const FAVORITE_ITEMS_QUERY_KEY = 'favorite_items_query_key';

const fetchFavoriteItems = async ({ page }: RequestQuery): Promise<PaginatedFavoriteItems> => {
  const url = PaginationURL('/favorites', page, 5, '');

  return fetch('GET', qualifyUrl(url));
};

const useFavoriteItems = (pagination: RequestQuery): { data: PaginatedFavoriteItems, isFetching: boolean } => useQuery(
  [FAVORITE_ITEMS_QUERY_KEY, pagination],
  () => defaultOnError(fetchFavoriteItems(pagination), 'Loading favorite items failed with status', 'Could not load favorite items'),
  {
    retry: 0,
    initialData: {
      favorites: [],
      ...DEFAULT_PAGINATION,
    },
  });

export default useFavoriteItems;
