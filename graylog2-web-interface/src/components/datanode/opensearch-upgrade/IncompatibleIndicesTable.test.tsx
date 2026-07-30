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
import { render, screen } from 'wrappedTestingLibrary';

import { SystemIndexerIndices } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import type { PaginatedEntityTableProps } from 'components/common/PaginatedEntityTable/PaginatedEntityTable';
import type { SearchParams } from 'stores/PaginationTypes';

import IncompatibleIndicesTable from './IncompatibleIndicesTable';
import { createColumnRenderers } from './IncompatibleIndicesColumnRenderers';
import IncompatibleIndicesContext from './IncompatibleIndicesContext';
import type { IncompatibleIndicesContextValue } from './IncompatibleIndicesContext';
import { fetchIncompatibleIndices, incompatibleIndicesKeyFn } from './fetchIncompatibleIndices';
import type { IncompatibleIndexRow } from './fetchIncompatibleIndices';
import useIncompatibleIndexActionState from './hooks/useIncompatibleIndexActionState';

jest.mock('components/common/PaginatedEntityTable', () => ({
  __esModule: true,
  default: jest.fn(({ humanName }) => <div>Paginated {humanName}</div>),
  useTableFetchContext: jest.fn(),
}));

jest.mock('@graylog/server-api', () => ({
  SystemIndexerIndices: { listOutdatedIndices: jest.fn() },
}));
jest.mock('./hooks/useIncompatibleIndexActionState');

const makeIndex = (overrides: Partial<IncompatibleIndexRow>): IncompatibleIndexRow => ({
  id: 'index',
  index_name: 'index',
  version: '7.10.2',
  warm_index: false,
  managed_index: false,
  system_index: false,
  active_write_index: null,
  begin: null,
  end: null,
  ...overrides,
});

const searchParams: SearchParams = {
  page: 2,
  pageSize: 20,
  query: '',
  sort: { attributeId: 'index_name', direction: 'asc' },
  filters: undefined,
};

describe('IncompatibleIndicesTable', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useIncompatibleIndexActionState).mockReturnValue({
      archiveActionsAvailable: true,
      archivedIndexNames: new Set(),
      pendingIndexStatuses: new Map(),
      addArchiveDeleteAction: jest.fn(),
      addReindexAction: jest.fn(),
      refetchClusterJobs: jest.fn(),
      refetch: jest.fn(),
    });
  });

  it('wires the paginated entity table to the outdated indices endpoint', () => {
    const { default: PaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(PaginatedEntityTable);

    render(<IncompatibleIndicesTable />);

    expect(screen.getByText('Paginated incompatible indices')).toBeInTheDocument();
    expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(1);

    const callProps = mockPaginatedEntityTable.mock.calls[0][0] as PaginatedEntityTableProps<
      IncompatibleIndexRow,
      unknown
    >;
    expect(callProps.fetchEntities).toBe(fetchIncompatibleIndices);
    expect(callProps.keyFn).toBe(incompatibleIndicesKeyFn);
    expect(callProps.entityAttributesAreCamelCase).toBe(false);
    expect(callProps.tableLayout.entityTableId).toBe('incompatible_indices');
    expect(typeof callProps.entityActions).toBe('function');
    expect(callProps.bulkSelection.actions).toBeTruthy();
    expect(callProps.columnRenderers.attributes).toHaveProperty('index_name');
  });
});

describe('fetchIncompatibleIndices', () => {
  it('maps the nested pagination total and adds the entity id', async () => {
    const index = {
      index_name: 'legacy-index',
      version: '7.10.2',
      warm_index: false,
      managed_index: false,
      system_index: false,
      active_write_index: null,
      begin: null,
      end: null,
    };
    const listOutdatedIndices = asMock(SystemIndexerIndices.listOutdatedIndices).mockResolvedValue({
      elements: [index],
      attributes: [],
      pagination: { total: 42 },
      total: 0,
    } as never);

    const result = await fetchIncompatibleIndices(searchParams);

    expect(listOutdatedIndices).toHaveBeenCalledWith('index_name', 2, 20, '', 'asc', {
      requestShouldExtendSession: false,
    });
    expect(result.list).toEqual([{ ...index, id: 'legacy-index' }]);
    expect(result.pagination).toEqual({ total: 42 });
  });
});

describe('createColumnRenderers', () => {
  const renderers = createColumnRenderers();

  const contextValue = (overrides: Partial<IncompatibleIndicesContextValue> = {}): IncompatibleIndicesContextValue => ({
    archiveActionsAvailable: false,
    archivedIndexNames: new Set(),
    pendingIndexStatuses: new Map(),
    addArchiveDeleteAction: jest.fn(),
    addReindexAction: jest.fn(),
    refetchClusterJobs: jest.fn(),
    refetch: jest.fn(),
    ...overrides,
  });

  const renderIndexNameCell = (index: IncompatibleIndexRow, ctx: Partial<IncompatibleIndicesContextValue> = {}) =>
    render(
      <IncompatibleIndicesContext.Provider value={contextValue(ctx)}>
        <div>{renderers.attributes.index_name.renderCell(index.index_name, index, undefined)}</div>
      </IncompatibleIndicesContext.Provider>,
    );

  it('falls back to "Unknown" for a missing version', () => {
    expect(renderers.attributes.version.renderCell(undefined, makeIndex({ version: '' }), undefined)).toBe('Unknown');
  });

  it('renders the index name with status badges (not type badges)', () => {
    const index = makeIndex({ index_name: 'graylog_2', warm_index: true, active_write_index: 'index-set-id' });

    renderIndexNameCell(index);

    expect(screen.getByText('graylog_2')).toBeInTheDocument();
    expect(screen.getByText('active write index')).toBeInTheDocument();
    expect(screen.queryByText('Warm')).not.toBeInTheDocument();
  });

  it('shows the "archived already" badge based on the context archived set', () => {
    const index = makeIndex({ index_name: 'graylog_2' });

    renderIndexNameCell(index, { archivedIndexNames: new Set(['graylog_2']) });

    expect(screen.getByText('archived already')).toBeInTheDocument();
  });

  it('does not show the "archived already" badge from a transient pending status alone', () => {
    const index = makeIndex({ index_name: 'graylog_2' });

    renderIndexNameCell(index, { pendingIndexStatuses: new Map([['graylog_2', { state: 'archived' }]]) });

    expect(screen.queryByText('archived already')).not.toBeInTheDocument();
  });

  it('renders the primary type as a single badge in the category column', () => {
    const cases: Array<[Partial<IncompatibleIndexRow>, string]> = [
      [{ managed_index: true }, 'Graylog'],
      [{ system_index: true }, 'System'],
      [{}, 'Foreign'],
    ];

    cases.forEach(([overrides, expected]) => {
      const { unmount } = render(
        <div>{renderers.attributes.category.renderCell(undefined, makeIndex(overrides), undefined)}</div>,
      );

      expect(screen.getByText(expected)).toBeInTheDocument();
      unmount();
    });
  });

  it('adds a Warm badge alongside the primary type', () => {
    const index = makeIndex({ managed_index: true, warm_index: true });

    render(<div>{renderers.attributes.category.renderCell(undefined, index, undefined)}</div>);

    expect(screen.getByText('Graylog')).toBeInTheDocument();
    expect(screen.getByText('Warm')).toBeInTheDocument();
  });

  it('renders an em dash for an unknown message range', () => {
    render(<div>{renderers.attributes.begin.renderCell(null, makeIndex({}), undefined)}</div>);

    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
