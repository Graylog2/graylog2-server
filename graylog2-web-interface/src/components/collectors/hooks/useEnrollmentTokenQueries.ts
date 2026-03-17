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
import type { SearchParams, Attribute } from 'stores/PaginationTypes';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import { defaultOnError } from 'util/conditional/onError';

import { OpAMPEnrollment } from '@graylog/server-api';

import type { EnrollmentTokenMetadata } from '../types';

export type PaginatedEnrollmentTokensResponse = {
  list: EnrollmentTokenMetadata[];
  pagination: { total: number };
  attributes: Attribute[];
};

export const ENROLLMENT_TOKENS_KEY_PREFIX = ['collectors', 'enrollment-tokens'];
export const enrollmentTokensKeyFn = (searchParams: SearchParams) => [...ENROLLMENT_TOKENS_KEY_PREFIX, 'paginated', searchParams];

export const fetchPaginatedEnrollmentTokens = async (
  searchParams: SearchParams,
): Promise<PaginatedEnrollmentTokensResponse> =>
  defaultOnError(
    OpAMPEnrollment.list(
      searchParams.page,
      searchParams.pageSize,
      searchParams.query,
      FiltersForQueryParams(searchParams.filters),
      searchParams.sort?.attributeId,
      searchParams.sort?.direction,
    ).then((response) => ({
      list: response.elements.map((el) => ({ ...el, id: el.id })),
      pagination: response.pagination,
      attributes: response.attributes,
    })),
    'Loading enrollment tokens failed with status',
    'Could not load enrollment tokens',
  );
