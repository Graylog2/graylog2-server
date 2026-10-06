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
import { OrderedMap } from 'immutable';

import type { UrlQueryFilters } from 'components/common/EntityFilters/types';

import {
  indexAttributes,
  healthBucket,
  matchesFilters,
  matchesQuery,
  NO_INDEX_SET,
  pageOf,
  sortIndices,
} from './indexList';
import type { IndexSummary } from './types';

const index = (name: string, overrides: Partial<IndexSummary> = {}): IndexSummary => ({
  index: name,
  health: 'green',
  status: 'open',
  tier: 'hot',
  index_set_id: 'set-1',
  index_set_title: 'Default index set',
  is_write_index: false,
  primary_shards: 1,
  replicas: 0,
  docs_count: 0,
  store_size_bytes: 208,
  ...overrides,
});

const names = (indices: Array<IndexSummary>) => indices.map((i) => i.index);

// As useUrlQueryFilters returns them: attribute id → selected values.
const filtersOf = (selected: { [attributeId: string]: Array<string> }): UrlQueryFilters => OrderedMap(selected);

describe('healthBucket', () => {
  it('uses the health, except for closed indices which have none', () => {
    expect(healthBucket(index('a', { health: 'yellow' }))).toBe('yellow');
    expect(healthBucket(index('b', { status: 'close', health: null }))).toBe('closed');
  });
});

describe('matchesFilters', () => {
  const red = index('graylog_20', { health: 'red', is_write_index: true });
  const warm = index('graylog_5', { tier: 'warm' });
  const foreign = index('security-auditlog', { index_set_id: null, index_set_title: null, health: 'yellow' });
  const closed = index('graylog_4', { status: 'close', health: null });

  it('matches everything without filters', () => {
    expect([red, warm, foreign, closed].every((i) => matchesFilters(i, filtersOf({})))).toBe(true);
  });

  it('ORs the values of one filter', () => {
    const filters = filtersOf({ health: ['red', 'yellow'] });

    expect(names([red, warm, foreign, closed].filter((i) => matchesFilters(i, filters)))).toEqual([
      'graylog_20',
      'security-auditlog',
    ]);
  });

  it('ANDs different filters', () => {
    const filters = filtersOf({ health: ['red', 'green'], write_index: ['true'] });

    expect(names([red, warm, foreign, closed].filter((i) => matchesFilters(i, filters)))).toEqual(['graylog_20']);
  });

  it('filters closed indices by their own health value, tier and missing index set', () => {
    expect(matchesFilters(closed, filtersOf({ health: ['closed'] }))).toBe(true);
    expect(matchesFilters(warm, filtersOf({ tier: ['warm'] }))).toBe(true);
    expect(matchesFilters(red, filtersOf({ tier: ['warm'] }))).toBe(false);
    expect(matchesFilters(foreign, filtersOf({ index_set: [NO_INDEX_SET] }))).toBe(true);
    expect(matchesFilters(red, filtersOf({ index_set: [NO_INDEX_SET] }))).toBe(false);
    expect(matchesFilters(red, filtersOf({ index_set: ['set-1'] }))).toBe(true);
  });

  it('narrows to exactly the picked index names, hyphens included', () => {
    const picked = filtersOf({ index: ['security-auditlog', 'graylog_2'] });

    expect(names([red, warm, foreign, closed].filter((i) => matchesFilters(i, picked)))).toEqual(['security-auditlog']);
  });

  it('ignores filters it does not know (e.g. from an old link)', () => {
    expect(matchesFilters(red, filtersOf({ no_such_attribute: ['x'] }))).toBe(true);
  });
});

describe('indexAttributes', () => {
  it('offers each index set present once, plus indices outside Graylog', () => {
    const attributes = indexAttributes([
      index('graylog_1'),
      index('graylog_2'),
      index('events_1', { index_set_id: 'set-2', index_set_title: 'Events' }),
      index('security-auditlog', { index_set_id: null, index_set_title: null }),
    ]);
    const indexSet = attributes.find((a) => a.id === 'index_set');

    expect(indexSet.filter_options).toEqual([
      { value: 'set-1', title: 'Default index set' },
      { value: 'set-2', title: 'Events' },
      { value: NO_INDEX_SET, title: 'Not managed by Graylog' },
    ]);
  });

  it('describes the filters as static, filterable string attributes; the index filter offers every index', () => {
    const attributes = indexAttributes([index('graylog_1'), index('graylog_2')]);
    const filters = attributes.filter((a) => a.filterable);

    expect(filters.map((a) => a.id)).toEqual(['index', 'health', 'tier', 'index_set', 'write_index']);
    expect(filters.every((a) => a.type === 'STRING' && a.filter_options.length > 0)).toBe(true);
    expect(attributes.find((a) => a.id === 'index').filter_options.map((o) => o.value)).toEqual(['graylog_1', 'graylog_2']);
    expect(attributes.find((a) => a.id === 'health').filter_options.map((o) => o.value)).toEqual([
      'green',
      'yellow',
      'red',
      'closed',
    ]);
  });

  it('makes every column sortable except the filter-only write index', () => {
    expect(
      indexAttributes([])
        .filter((a) => !a.sortable)
        .map((a) => a.id),
    ).toEqual(['write_index']);
  });
});

describe('matchesQuery', () => {
  it('searches index names and index set titles, ignoring case', () => {
    expect(matchesQuery(index('graylog_20'), 'LOG_2')).toBe(true);
    expect(matchesQuery(index('graylog_20'), 'default')).toBe(true);
    expect(matchesQuery(index('security-auditlog', { index_set_title: null }), 'default')).toBe(false);
    expect(matchesQuery(index('graylog_20'), '')).toBe(true);
  });
});

describe('sortIndices', () => {
  const indices = [
    index('graylog_10', { health: 'green', docs_count: 5 }),
    index('graylog_2', { health: 'red', docs_count: null }),
    index('graylog_9', { health: 'yellow', docs_count: 20 }),
    index('graylog_4', { status: 'close', health: null, docs_count: null }),
  ];

  it('orders names naturally (graylog_2 before graylog_10)', () => {
    expect(names(sortIndices(indices, { field: 'index', direction: 'asc' }))).toEqual([
      'graylog_2',
      'graylog_4',
      'graylog_9',
      'graylog_10',
    ]);
  });

  it('puts the worst health first when ascending, closed last', () => {
    expect(names(sortIndices(indices, { field: 'health', direction: 'asc' }))).toEqual([
      'graylog_2',
      'graylog_9',
      'graylog_10',
      'graylog_4',
    ]);
  });

  it('keeps missing values last in both directions', () => {
    expect(names(sortIndices(indices, { field: 'docs_count', direction: 'desc' }))).toEqual([
      'graylog_9',
      'graylog_10',
      'graylog_2',
      'graylog_4',
    ]);
    expect(names(sortIndices(indices, { field: 'docs_count', direction: 'asc' }))).toEqual([
      'graylog_10',
      'graylog_9',
      'graylog_2',
      'graylog_4',
    ]);
  });

  it('does not change the input', () => {
    const copy = [...indices];
    sortIndices(indices, { field: 'health', direction: 'desc' });

    expect(indices).toEqual(copy);
  });
});

describe('pageOf', () => {
  const rows = Array.from({ length: 45 }, (_, i) => i);

  it('slices the requested page', () => {
    expect(pageOf(rows, 2, 20)).toEqual({ totalPages: 3, currentPage: 2, rows: rows.slice(20, 40) });
    expect(pageOf(rows, 3, 20).rows).toEqual(rows.slice(40));
  });

  it('shows the last page when the requested one no longer exists', () => {
    expect(pageOf(rows.slice(0, 15), 3, 10)).toEqual({ totalPages: 2, currentPage: 2, rows: rows.slice(10, 15) });
  });

  it('has one empty page for no rows', () => {
    expect(pageOf([], 1, 20)).toEqual({ totalPages: 1, currentPage: 1, rows: [] });
  });
});
