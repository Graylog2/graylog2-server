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
import { SystemIndexerIndices } from '@graylog/server-api';

import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';

export type IncompatibleIndexRow = IncompatibleIndex & { id: string };

export type IncompatibleIndicesResponse = {
  list: Array<IncompatibleIndexRow>;
  pagination: { total: number };
  attributes: Array<Attribute>;
};

export const INCOMPATIBLE_INDICES_QUERY_KEY = ['incompatibleIndices'] as const;

type ListOutdatedIndicesSort = Parameters<typeof SystemIndexerIndices.listOutdatedIndices>[0];
type ListOutdatedIndicesOrder = Parameters<typeof SystemIndexerIndices.listOutdatedIndices>[4];

export const fetchIncompatibleIndices = (searchParams: SearchParams): Promise<IncompatibleIndicesResponse> => {
  const query = [searchParams.query, ...(FiltersForQueryParams(searchParams.filters) ?? [])].filter(Boolean).join(' ');
  const sort = (searchParams.sort?.attributeId ?? 'index_name') as ListOutdatedIndicesSort;
  const order = (searchParams.sort?.direction ?? 'asc') as ListOutdatedIndicesOrder;

  return SystemIndexerIndices.listOutdatedIndices(sort, searchParams.page, searchParams.pageSize, query, order).then(
    ({ elements, attributes, pagination }) => ({
      list: elements.map((element) => ({ ...element, id: element.index_name })) as Array<IncompatibleIndexRow>,
      pagination: { total: pagination.total },
      attributes,
    }),
  );
};

export const incompatibleIndicesKeyFn = (searchParams?: SearchParams) => [
  ...INCOMPATIBLE_INDICES_QUERY_KEY,
  'paginated',
  ...(searchParams ? [searchParams] : []),
];
