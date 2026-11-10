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
import { SystemIndexSets } from '@graylog/server-api';

import type { SearchParams } from 'stores/PaginationTypes';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';

import { filterCloudHiddenAttributes } from './Constants';
import type { IndexSetEntity } from './types';

type SortField = Parameters<typeof SystemIndexSets.getPage>[4];

export const KEY_PREFIX = ['indexSets', 'overview'];
export const keyFn = (searchParams: SearchParams) => [...KEY_PREFIX, searchParams];

export const fetchIndexSets = (searchParams: SearchParams): Promise<PaginatedResponse<IndexSetEntity>> =>
  SystemIndexSets.getPage(
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    FiltersForQueryParams(searchParams.filters),
    searchParams.sort.attributeId as SortField,
    searchParams.sort.direction,
  ).then((response) => ({
    list: response.elements as unknown as Array<IndexSetEntity>,
    attributes: filterCloudHiddenAttributes(response.attributes),
    pagination: { total: response.total },
  }));
