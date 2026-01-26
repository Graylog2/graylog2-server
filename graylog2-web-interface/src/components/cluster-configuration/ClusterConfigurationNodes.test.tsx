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

import { SEARCH_DEBOUNCE_THRESHOLD } from 'components/common/SearchForm';
import asMock from 'helpers/mocking/AsMock';

import ClusterConfigurationNodes from './ClusterConfigurationNodes';

jest.mock('components/common/PaginatedEntityTable', () => ({
  __esModule: true,
  default: jest.fn(() => <div role="table">paginated-table</div>),
  useTableFetchContext: jest.fn(),
}));

describe('<ClusterConfigurationNodes />', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('renders both node types with default paging and refresh settings in "all" view', () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<ClusterConfigurationNodes />);

    expect(screen.getAllByRole('table')).toHaveLength(2);
    expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(2);
  });

  it('switches to a specific node type when segmented control is used', async () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<ClusterConfigurationNodes />);

    mockPaginatedEntityTable.mockClear();

    await userEvent.click(screen.getByRole('radio', { name: 'Data Nodes' }));

    await waitFor(() => expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(1));
  });

  it('uses child "select node type" handler to switch view', async () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<ClusterConfigurationNodes />);

    mockPaginatedEntityTable.mockClear();

    await userEvent.click(screen.getByRole('radio', { name: 'Data Nodes' }));

    await waitFor(() => expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(1));
  });

  it('passes trimmed search query to children', async () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<ClusterConfigurationNodes />);

    const searchInput = screen.getByPlaceholderText('Search nodes…');

    await userEvent.type(searchInput, '  nodes  ');
    act(() => {
      jest.advanceTimersByTime(SEARCH_DEBOUNCE_THRESHOLD + 10);
    });

    await waitFor(() => expect(mockPaginatedEntityTable).toHaveBeenCalled());
  });
});
