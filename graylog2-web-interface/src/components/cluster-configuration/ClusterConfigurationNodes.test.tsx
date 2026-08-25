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
import { defaultUser } from 'defaultMockValues';

import { Datanode } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import useCurrentUser from 'hooks/useCurrentUser';

import ClusterConfigurationNodes from './ClusterConfigurationNodes';

type MockPaginatedEntityTableProps = {
  humanName?: string;
  onDataLoaded?: (data: { list: Array<unknown>; pagination?: { total?: number } }) => void;
  externalSearch?: { query?: string };
};

jest.mock('@graylog/server-api', () => ({
  Datanode: {
    runsWithDataNode: jest.fn(),
  },
}));

jest.mock('hooks/useCurrentUser');

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
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(Datanode.runsWithDataNode).mockResolvedValue(true);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders Data Nodes instead of OpenSearch Nodes when using Data Node', async () => {
    render(<ClusterConfigurationNodes />);

    await waitFor(() => expect(screen.getAllByRole('table')).toHaveLength(3));
    expect(screen.getByRole('radio', { name: 'Data Nodes' })).toBeInTheDocument();
    expect(screen.queryByRole('radio', { name: 'OpenSearch Nodes' })).not.toBeInTheDocument();
    expect(Datanode.runsWithDataNode).toHaveBeenCalledWith({ requestShouldExtendSession: false });
  });

  it('switches to a specific node type when segmented control is used', async () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<ClusterConfigurationNodes />);

    await screen.findByRole('radio', { name: 'Data Nodes' });
    mockPaginatedEntityTable.mockClear();

    await userEvent.click(screen.getByRole('radio', { name: 'Data Nodes' }));

    await waitFor(() => expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(1));
  });

  it('switches to mongodb node type when segmented control is used', async () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<ClusterConfigurationNodes />);

    await screen.findByRole('radio', { name: 'Data Nodes' });
    mockPaginatedEntityTable.mockClear();

    await userEvent.click(screen.getByRole('radio', { name: 'MongoDB Nodes' }));

    await waitFor(() => expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(1));
  });

  it('switches to OpenSearch node type when segmented control is used', async () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);
    asMock(Datanode.runsWithDataNode).mockResolvedValue(false);

    render(<ClusterConfigurationNodes />);

    await waitFor(() => expect(screen.getAllByRole('table')).toHaveLength(3));
    expect(screen.queryByRole('radio', { name: 'Data Nodes' })).not.toBeInTheDocument();
    mockPaginatedEntityTable.mockClear();

    await userEvent.click(screen.getByRole('radio', { name: 'OpenSearch Nodes' }));

    await waitFor(() => expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(1));
    expect(mockPaginatedEntityTable.mock.calls[0][0]).toEqual(
      expect.objectContaining({ humanName: 'OpenSearch Nodes' }),
    );
  });

  it('keeps unrelated node tables visible when search backend detection fails', async () => {
    asMock(Datanode.runsWithDataNode).mockRejectedValue(new Error('Request failed'));

    render(<ClusterConfigurationNodes />);

    await screen.findByText('Could not determine the configured search backend.');
    expect(screen.getAllByRole('table')).toHaveLength(2);
    expect(screen.getByRole('radio', { name: 'Graylog Nodes' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'MongoDB Nodes' })).toBeInTheDocument();
  });

  it('does not request search backend configuration without permission', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions([]).build());

    render(<ClusterConfigurationNodes />);

    await screen.findByText('Could not determine the configured search backend.');
    expect(screen.getAllByRole('table')).toHaveLength(2);
    expect(Datanode.runsWithDataNode).not.toHaveBeenCalled();
  });

  it('uses child "select node type" handler to switch view', async () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<ClusterConfigurationNodes />);

    await screen.findByRole('radio', { name: 'Data Nodes' });
    const calls = mockPaginatedEntityTable.mock.calls as Array<[MockPaginatedEntityTableProps]>;
    const dataNodesTableProps = calls.map(([props]) => props).find((props) => props?.humanName === 'Data Nodes');

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
    await screen.findByRole('radio', { name: 'Data Nodes' });
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
