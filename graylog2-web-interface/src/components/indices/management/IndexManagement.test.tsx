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
import * as React from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { OrderedMap } from 'immutable';

import { IndexerIndicesManagement } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import { adminUser } from 'fixtures/users';
import useCurrentUser from 'hooks/useCurrentUser';
import useUrlQueryFilters from 'components/common/EntityFilters/hooks/useUrlQueryFilters';
import type { PaginatedEntityTableProps } from 'components/common/PaginatedEntityTable/PaginatedEntityTable';
import type { SearchParams } from 'stores/PaginationTypes';

import IndexManagement from './IndexManagement';
import { fetchIndices } from './fetchIndices';
import type { IndexRow } from './fetchIndices';
import type { IndexSummary } from './types';

jest.mock('components/common/PaginatedEntityTable', () => ({
  __esModule: true,
  // Renders what the page hands it for the table's toolbar.
  default: jest.fn(({ humanName, topRightCol }) => (
    <div>
      Paginated {humanName}
      {topRightCol}
    </div>
  )),
  useTableFetchContext: jest.fn(),
}));

jest.mock('@graylog/server-api', () => ({
  IndexerIndicesManagement: { list: jest.fn() },
  IndexerIndicesManagementActions: {},
  IndexerIndicesManagementAllocation: {},
  ClusterDeflector: {},
}));

jest.mock('hooks/useCurrentUser');
jest.mock('components/common/EntityFilters/hooks/useUrlQueryFilters');

// The panel has its own tests; here it only needs to pick an index.
jest.mock('./allocation/AllocationPanel', () => ({
  __esModule: true,
  default: ({ onTogglePicked }: { onTogglePicked: (index: string) => void }) => (
    <button type="button" onClick={() => onTogglePicked('graylog_1')}>
      Pick graylog_1
    </button>
  ),
}));

const index = (name: string, overrides: Partial<IndexSummary> = {}): IndexSummary => ({
  index: name,
  health: 'green',
  status: 'open',
  tier: 'hot',
  primary_shards: 1,
  replicas: 0,
  docs_count: 10,
  store_size_bytes: 208,
  index_set_id: 'set-1',
  index_set_title: 'Default index set',
  is_write_index: false,
  ...overrides,
});

const INDICES = [index('graylog_1'), index('graylog_2', { health: 'red' }), index('security-auditlog', { index_set_id: null })];

const searchParams = (overrides: Partial<SearchParams> = {}): SearchParams => ({
  page: 1,
  pageSize: 20,
  query: '',
  sort: { attributeId: 'index', direction: 'asc' },
  filters: OrderedMap(),
  ...overrides,
});

const lastTableProps = () => {
  const { default: PaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
  const { calls } = asMock(PaginatedEntityTable).mock;

  return calls[calls.length - 1][0] as PaginatedEntityTableProps<IndexRow, unknown>;
};

describe('IndexManagement', () => {
  const setFilters = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCurrentUser).mockReturnValue(adminUser);
    asMock(useUrlQueryFilters).mockReturnValue([OrderedMap(), setFilters]);
    asMock(IndexerIndicesManagement.list).mockResolvedValue({ indices: INDICES });
  });

  it('lists indices in core\'s paginated entity table, with row and bulk actions', async () => {
    render(<IndexManagement />);

    expect(await screen.findByText('Paginated indices')).toBeInTheDocument();
    await waitFor(() => expect(lastTableProps().bulkSelection?.actions).toBeTruthy());

    const props = lastTableProps();
    expect(props.tableLayout.entityTableId).toBe('index_management');
    expect(props.entityAttributesAreCamelCase).toBe(false);
    expect(typeof props.entityActions).toBe('function');
    expect(props.columnRenderers.attributes).toHaveProperty('index');
    expect(props.middleSection).toBeUndefined();
  });

  it('makes the list wait for a pick, search or filter while the allocation panel is open', async () => {
    render(<IndexManagement />);

    await userEvent.click(await screen.findByRole('button', { name: 'Explain allocation' }));

    expect(screen.getByRole('button', { name: 'Explain allocation' })).toBeDisabled();
    expect(lastTableProps().middleSection).toBeDefined();
  });

  it("turns a pick in the allocation panel into the list's index filter", async () => {
    render(<IndexManagement />);
    await userEvent.click(await screen.findByRole('button', { name: 'Explain allocation' }));

    await userEvent.click(screen.getByRole('button', { name: 'Pick graylog_1' }));

    expect(setFilters).toHaveBeenLastCalledWith(OrderedMap({ index: ['graylog_1'] }));
  });

  it('removes an index from the filter when it is picked again', async () => {
    asMock(useUrlQueryFilters).mockReturnValue([OrderedMap({ index: ['graylog_1'], health: ['red'] }), setFilters]);
    render(<IndexManagement />);
    await userEvent.click(await screen.findByRole('button', { name: 'Explain allocation' }));

    await userEvent.click(screen.getByRole('button', { name: 'Pick graylog_1' }));

    expect(setFilters).toHaveBeenLastCalledWith(OrderedMap({ health: ['red'] }));
  });

  it('offers explain and retry only to users permitted to use them', async () => {
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().permissions(['indices:read']).build());

    render(<IndexManagement />);

    expect(await screen.findByText('Paginated indices')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Explain allocation' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Retry failed allocations' })).not.toBeInTheDocument();
  });
});

describe('fetchIndices', () => {
  beforeEach(() => {
    asMock(IndexerIndicesManagement.list).mockResolvedValue({ indices: INDICES });
  });

  it('searches, filters, sorts and pages the full list in the browser', async () => {
    const { list, pagination, attributes } = await fetchIndices(
      searchParams({ query: 'log', sort: { attributeId: 'health', direction: 'asc' }, pageSize: 1 }),
    );

    // "log" matches all three; red first when sorting by health ascending; one per page.
    expect(pagination.total).toBe(3);
    expect(list.map((row) => [row.id, row.index])).toEqual([['graylog_2', 'graylog_2']]);
    expect(attributes.map((attribute) => attribute.id)).toContain('index_set');

    const filtered = await fetchIndices(searchParams({ filters: OrderedMap({ index_set: ['none'] }) }));
    expect(filtered.list.map((row) => row.index)).toEqual(['security-auditlog']);
  });

  it('answers with no rows, but the attributes, while waiting for something to narrow the list', async () => {
    const waiting = await fetchIndices(searchParams(), true);
    expect(waiting.list).toEqual([]);
    expect(waiting.attributes.length).toBeGreaterThan(0);

    const picked = await fetchIndices(searchParams({ filters: OrderedMap({ index: ['graylog_1'] }) }), true);
    expect(picked.list.map((row) => row.index)).toEqual(['graylog_1']);
  });
});
