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
import { IndexerIndicesManagement } from '@graylog/server-api';

import type { Attributes, SearchParams } from 'stores/PaginationTypes';

import { indexAttributes, matchesFilters, matchesQuery, pageOf, sortIndices } from './indexList';
import type { IndexSummary, SortField } from './types';

export type IndexRow = IndexSummary & { id: string };

export type IndicesResponse = {
  list: Array<IndexRow>;
  pagination: { total: number };
  attributes: Attributes;
};

export const INDICES_QUERY_KEY = ['indices', 'management', 'list'] as const;

const isNarrowed = ({ query, filters }: SearchParams) => Boolean(query) || (filters?.size ?? 0) > 0;

/**
 * For core's PaginatedEntityTable. The server answers with every index at once (one _cat/indices call), so search,
 * filters, sorting and paging happen here, with the tested helpers of indexList.ts. With `waitForNarrowing` (while the
 * allocation panel is open) the list stays empty until a search, filter or picked index narrows it.
 */
export const fetchIndices = async (searchParams: SearchParams, waitForNarrowing = false): Promise<IndicesResponse> => {
  const { indices } = await IndexerIndicesManagement.list({ requestShouldExtendSession: false });
  const attributes = indexAttributes(indices);

  if (waitForNarrowing && !isNarrowed(searchParams)) {
    return { list: [], pagination: { total: 0 }, attributes };
  }

  const matching = indices.filter(
    (index) => matchesQuery(index, searchParams.query) && matchesFilters(index, searchParams.filters),
  );
  const sorted = sortIndices(matching, {
    field: (searchParams.sort?.attributeId ?? 'index') as SortField,
    direction: searchParams.sort?.direction ?? 'asc',
  });
  const { rows } = pageOf(sorted, searchParams.page, searchParams.pageSize);

  return {
    list: rows.map((index) => ({ ...index, id: index.index })),
    pagination: { total: matching.length },
    attributes,
  };
};

export const indicesKeyFn = (searchParams: SearchParams, waitForNarrowing = false) => [
  ...INDICES_QUERY_KEY,
  { ...searchParams, waitForNarrowing },
];
