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
import userEvent from '@testing-library/user-event';
import React from 'react';
import { act, render, screen, waitFor } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';

import ClusterConfigurationNodes from './ClusterConfigurationNodes';

type MockPaginatedEntityTableProps = {
  humanName?: string;
  onDataLoaded?: (data: { list: Array<unknown>; pagination?: { total?: number } }) => void;
  externalSearch?: { query?: string };
};

jest.mock('components/common/PaginatedEntityTable', () => ({
  __esModule: true,
  default: jest.fn(() => <div role="table">paginated-table</div>),
  useTableFetchContext: jest.fn(),
}));

jest.mock('./mongodb-nodes/useMongodbProfilingToggle', () => ({
  __esModule: true,
  default: jest.fn(() => ({
    action: 'enable',
    state: 'off',
    profilingStatusByLevel: { OFF: 3 },
    isStatusReady: true,
    isTogglingProfiling: false,
    runToggleAction: jest.fn(),
  })),
}));

describe('<ClusterConfigurationNodes />', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders all node types with default paging and refresh settings in "all" view', () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<ClusterConfigurationNodes />);

    expect(screen.getAllByRole('table')).toHaveLength(3);
    expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(3);
  });

  it('switches to a specific node type when segmented control is used', async () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<ClusterConfigurationNodes />);

    mockPaginatedEntityTable.mockClear();

    await userEvent.click(screen.getByRole('radio', { name: 'Data Nodes' }));

    await waitFor(() => expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(1));
  });

  it('switches to mongodb node type when segmented control is used', async () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<ClusterConfigurationNodes />);

    mockPaginatedEntityTable.mockClear();

    await userEvent.click(screen.getByRole('radio', { name: 'MongoDB Nodes' }));

    await waitFor(() => expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(1));
  });

  it('uses child "select node type" handler to switch view', async () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<ClusterConfigurationNodes />);

    const calls = mockPaginatedEntityTable.mock.calls as Array<[MockPaginatedEntityTableProps]>;
    const dataNodesTableProps = calls
      .map(([props]) => props)
      .find((props) => props?.humanName === 'Data Nodes');

    expect(dataNodesTableProps?.onDataLoaded).toBeDefined();

    if (dataNodesTableProps?.onDataLoaded) {
      act(() => {
        dataNodesTableProps.onDataLoaded?.({ list: [], pagination: { total: 3 } });
      });
    }

    mockPaginatedEntityTable.mockClear();

    await userEvent.click(screen.getByRole('button', { name: 'Show Data Nodes' }));

    await waitFor(() => expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(1));
  });

  it('passes trimmed search query to children', async () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<ClusterConfigurationNodes />);
    mockPaginatedEntityTable.mockClear();

    const searchInput = screen.getByPlaceholderText('Search nodes…');

    await userEvent.type(searchInput, '  nodes  ');

    await waitFor(() => {
      expect(mockPaginatedEntityTable).toHaveBeenCalled();
      const calls = mockPaginatedEntityTable.mock.calls as Array<[MockPaginatedEntityTableProps]>;
      const queries = calls.map(([props]) => props.externalSearch?.query);

      expect(queries.every((query) => query === 'nodes')).toBe(true);
    });
  });
});
