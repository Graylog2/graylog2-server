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
// The Index Management list's logic: filtering, sorting, filter options and paging. Only type imports from Graylog,
// so it can be unit-tested on its own (indexList.test.ts).
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import type { Attributes } from 'stores/PaginationTypes';

import type { IndexSummary, Sort, SortField } from './types';

// Closed indices have no health; they get their own value.
export const healthBucket = (index: IndexSummary): string => (index.status === 'close' ? 'closed' : index.health);

// Filters: core's EntityFilters with static value lists ("Filters" dropdown + removable chips, state in the URL's
// `filters` query parameter). Values of one attribute are OR-ed, attributes are AND-ed, as in core's tables.
export const NO_INDEX_SET = 'none';

const FILTER_VALUE: { [attributeId: string]: (index: IndexSummary) => string } = {
  health: healthBucket,
  tier: (index) => index.tier,
  index_set: (index) => index.index_set_id ?? NO_INDEX_SET,
  write_index: (index) => String(Boolean(index.is_write_index)),
};

export const filterAttributes = (indices: Array<IndexSummary>): Attributes => {
  const indexSets = new Map<string, string>();
  indices.forEach((index) => {
    if (index.index_set_id) {
      indexSets.set(index.index_set_id, index.index_set_title ?? index.index_set_id);
    }
  });

  return [
    {
      id: 'health',
      title: 'Health',
      type: 'STRING',
      filterable: true,
      filter_options: [
        { value: 'green', title: 'Green' },
        { value: 'yellow', title: 'Yellow' },
        { value: 'red', title: 'Red' },
        { value: 'closed', title: 'Closed' },
      ],
    },
    {
      id: 'tier',
      title: 'Tier',
      type: 'STRING',
      filterable: true,
      filter_options: [
        { value: 'hot', title: 'Hot' },
        { value: 'warm', title: 'Warm' },
      ],
    },
    {
      id: 'index_set',
      title: 'Index set',
      type: 'STRING',
      filterable: true,
      filter_options: [
        ...[...indexSets].map(([value, title]) => ({ value, title })),
        { value: NO_INDEX_SET, title: 'Not managed by Graylog' },
      ],
    },
    {
      id: 'write_index',
      title: 'Write index',
      type: 'STRING',
      filterable: true,
      filter_options: [
        { value: 'true', title: 'Yes' },
        { value: 'false', title: 'No' },
      ],
    },
  ];
};

// `filters` (from useUrlQueryFilters) maps attribute id → selected values.
export const matchesFilters = (index: IndexSummary, filters: UrlQueryFilters) =>
  filters.every((values, attributeId) => {
    const valueOf = FILTER_VALUE[attributeId];

    return !valueOf || values.includes(valueOf(index));
  });

export const matchesQuery = (index: IndexSummary, query: string) => {
  if (!query) {
    return true;
  }

  const needle = query.toLowerCase();

  return index.index.toLowerCase().includes(needle) || (index.index_set_title ?? '').toLowerCase().includes(needle);
};

// Sorting: worst health first when ascending; unknown values (null, closed index sizes) always last.
const HEALTH_ORDER: { [health: string]: number } = { red: 0, yellow: 1, green: 2, closed: 3 };
const TIER_ORDER: { [tier: string]: number } = { hot: 0, warm: 1 };
const compareText = (a: string, b: string) => a.localeCompare(b, undefined, { numeric: true, sensitivity: 'base' });
const compareNumber = (a: number, b: number) => a - b;

type SortSpec<T> = { value: (index: IndexSummary) => T | null | undefined; compare: (a: T, b: T) => number };

const SORT_FIELDS: { [field in SortField]: SortSpec<string> | SortSpec<number> } = {
  index: { value: (index) => index.index, compare: compareText },
  health: { value: (index) => HEALTH_ORDER[healthBucket(index)], compare: compareNumber },
  tier: { value: (index) => TIER_ORDER[index.tier] ?? 9, compare: compareNumber },
  index_set: { value: (index) => index.index_set_title, compare: compareText },
  primary_shards: { value: (index) => index.primary_shards, compare: compareNumber },
  replicas: { value: (index) => index.replicas, compare: compareNumber },
  docs_count: { value: (index) => index.docs_count, compare: compareNumber },
  store_size_bytes: { value: (index) => index.store_size_bytes, compare: compareNumber },
};

export const sortIndices = (indices: Array<IndexSummary>, { field, direction }: Sort) => {
  const { value, compare } = SORT_FIELDS[field] as SortSpec<string | number>;
  const factor = direction === 'asc' ? 1 : -1;

  return [...indices].sort((a, b) => {
    const left = value(a);
    const right = value(b);
    const leftMissing = left === null || left === undefined;
    const rightMissing = right === null || right === undefined;

    if (leftMissing && rightMissing) {
      return compareText(a.index, b.index);
    }

    if (leftMissing || rightMissing) {
      return leftMissing ? 1 : -1;
    }

    return factor * compare(left, right) || compareText(a.index, b.index);
  });
};

// Paging happens in the browser, like filtering and sorting: the server returns every index at once.
// A page past the end (after deletions shrink the list) shows the last page instead.
export const pageOf = <T>(rows: Array<T>, page: number, pageSize: number) => {
  const totalPages = Math.max(1, Math.ceil(rows.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const start = (currentPage - 1) * pageSize;

  return { totalPages, currentPage, rows: rows.slice(start, start + pageSize) };
};
