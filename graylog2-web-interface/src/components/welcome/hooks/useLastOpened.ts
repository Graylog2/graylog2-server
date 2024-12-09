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

import type { PaginatedLastOpened, RequestQuery } from 'components/welcome/types';
import PaginationURL from 'util/PaginationURL';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { DEFAULT_PAGINATION } from 'components/welcome/Constants';
import { defaultOnError } from 'util/conditional/onError';

const urlPrefix = '/startpage';

export const LAST_OPEN_QUERY_KEY = 'last_open_query_key';

const fetchLastOpen = async ({ page }: RequestQuery): Promise<PaginatedLastOpened> => {
  const url = PaginationURL(`${urlPrefix}/lastOpened`, page, 5, '');

  return fetch('GET', qualifyUrl(url));
};

const useLastOpened = (pagination: RequestQuery): { data: PaginatedLastOpened, isFetching: boolean } => useQuery(
  [LAST_OPEN_QUERY_KEY, pagination],
  () => defaultOnError(fetchLastOpen(pagination), 'Loading last opened items failed with status', 'Could not load last opened items'),
  {
    retry: 0,
    initialData: {
      lastOpened: [],
      ...DEFAULT_PAGINATION,
    },
  });

export default useLastOpened;
