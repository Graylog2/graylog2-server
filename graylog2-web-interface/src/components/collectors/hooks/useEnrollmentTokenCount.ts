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

import type { SearchParams } from 'stores/PaginationTypes';

import { enrollmentTokensKeyFn, fetchPaginatedEnrollmentTokens } from './useEnrollmentTokenQueries';

// Minimal page: only the pagination total is needed (e.g. for the tab badge on the deployment page).
const COUNT_SEARCH_PARAMS: SearchParams = {
  page: 1,
  pageSize: 1,
  query: '',
  sort: { attributeId: 'created_at', direction: 'desc' },
};

const useEnrollmentTokenCount = (): number | undefined => {
  const { data } = useQuery({
    queryKey: enrollmentTokensKeyFn(COUNT_SEARCH_PARAMS),
    queryFn: () => fetchPaginatedEnrollmentTokens(COUNT_SEARCH_PARAMS),
  });

  return data?.pagination?.total;
};

export default useEnrollmentTokenCount;
