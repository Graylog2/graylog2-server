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
import React from 'react';
import { act, fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';

import { SEARCH_DEBOUNCE_THRESHOLD } from 'components/common/SearchForm';

import ClusterConfigurationNodes from './ClusterConfigurationNodes';
import useClusterDataNodes from './data-nodes/useClusterDataNodes';
import useClusterDataNodesTableLayout from './data-nodes/useClusterDataNodesTableLayout';
import useClusterGraylogNodes from './graylog-nodes/useClusterGraylogNodes';
import useClusterGraylogNodesTableLayout from './graylog-nodes/useClusterGraylogNodesTableLayout';

jest.mock('./data-nodes/useClusterDataNodes');
jest.mock('./data-nodes/useClusterDataNodesTableLayout');
jest.mock('./graylog-nodes/useClusterGraylogNodes');
jest.mock('./graylog-nodes/useClusterGraylogNodesTableLayout');
jest.mock('./data-nodes/useAddMetricsToDataNodes');
jest.mock('./graylog-nodes/useAddMetricsToGraylogNodes');

describe('<ClusterConfigurationNodes />', () => {
  const defaultDataLayout = {
    defaultDisplayedColumns: ['hostname'],
    defaultColumnOrder: ['hostname'],
    layoutPreferences: {
      attributes: undefined,
      order: undefined,
      pageSize: 0,
      sort: { attributeId: 'hostname', direction: 'asc' },
    },
    searchParams: { sort: { attributeId: 'hostname', direction: 'asc' }, query: '' },
    isLoadingLayout: false,
    handleLayoutPreferencesChange: jest.fn(),
    handleSortChange: jest.fn(),
  };

  const defaultGraylogLayout = {
    defaultDisplayedColumns: ['hostname'],
    defaultColumnOrder: ['hostname'],
    layoutPreferences: {
      attributes: undefined,
      order: undefined,
      pageSize: 0,
      sort: { attributeId: 'hostname', direction: 'asc' },
    },
    searchParams: { sort: { attributeId: 'hostname', direction: 'asc' }, query: '' },
    isLoadingLayout: false,
    handleLayoutPreferencesChange: jest.fn(),
    handleSortChange: jest.fn(),
  };

  beforeEach(() => {
    jest.useFakeTimers();
    (useClusterDataNodesTableLayout as jest.Mock).mockReturnValue(defaultDataLayout);
    (useClusterGraylogNodesTableLayout as jest.Mock).mockReturnValue(defaultGraylogLayout);
    (useClusterDataNodes as jest.Mock).mockReturnValue({
      nodes: [{ id: 'data-1', hostname: 'data-host', metrics: {} }],
      total: 1,
      refetch: jest.fn(),
      isLoading: false,
      setPollingEnabled: jest.fn(),
      pollingEnabled: true,
    });
    (useClusterGraylogNodes as jest.Mock).mockReturnValue({
      nodes: [{ id: 'graylog-1', hostname: 'graylog-host', metrics: {} }],
      total: 1,
      refetch: jest.fn(),
      isLoading: false,
      setPollingEnabled: jest.fn(),
      pollingEnabled: true,
    });
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('renders both node types with default paging and refresh settings in "all" view', () => {
    render(<ClusterConfigurationNodes />);

    expect(screen.getAllByRole('table')).toHaveLength(2);
    expect(useClusterDataNodesTableLayout).toHaveBeenCalledWith('', 10);
    expect(useClusterGraylogNodesTableLayout).toHaveBeenCalledWith('', 10);
    expect(useClusterDataNodes).toHaveBeenCalledWith(defaultDataLayout.searchParams, { refetchInterval: 5000 });
    expect(useClusterGraylogNodes).toHaveBeenCalledWith(defaultGraylogLayout.searchParams, { refetchInterval: 5000 });
  });

  it('switches to a specific node type when segmented control is used', async () => {
    render(<ClusterConfigurationNodes />);

    fireEvent.click(screen.getByRole('radio', { name: 'Data Nodes' }));

    await waitFor(() => {
      expect(useClusterDataNodesTableLayout).toHaveBeenLastCalledWith(expect.anything(), 100);
    });

    expect(useClusterGraylogNodesTableLayout).toHaveBeenCalledTimes(1);
    expect(useClusterDataNodes).toHaveBeenLastCalledWith(expect.objectContaining({ query: '' }), {
      refetchInterval: 10000,
    });
  });

  it('uses child "select node type" handler to switch view', async () => {
    render(<ClusterConfigurationNodes />);

    fireEvent.click(screen.getByRole('radio', { name: 'Data Nodes' }));

    await waitFor(() => {
      expect(useClusterDataNodesTableLayout).toHaveBeenLastCalledWith('', 100);
    });
  });

  it('passes trimmed search query to children', async () => {
    render(<ClusterConfigurationNodes />);

    const searchInput = screen.getByPlaceholderText('Search nodes…');

    fireEvent.change(searchInput, { target: { value: '  nodes  ' } });
    act(() => {
      jest.advanceTimersByTime(SEARCH_DEBOUNCE_THRESHOLD + 10);
    });

    await waitFor(() => expect(useClusterDataNodesTableLayout).toHaveBeenLastCalledWith('nodes', expect.anything()));
    expect(useClusterGraylogNodesTableLayout).toHaveBeenLastCalledWith('nodes', expect.anything());
  });
});
