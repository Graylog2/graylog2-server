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
import { render, screen } from 'wrappedTestingLibrary';

import GraylogNodesExpandable from './GraylogNodesExpandable';
import useClusterGraylogNodes from './useClusterGraylogNodes';
import useClusterGraylogNodesTableLayout from './useClusterGraylogNodesTableLayout';

jest.mock('./useClusterGraylogNodes');
jest.mock('./useClusterGraylogNodesTableLayout');

const defaultLayout = {
  columnsOrder: ['hostname'],
  columnPreferences: undefined,
  defaultDisplayedColumns: ['hostname'],
  searchParams: { sort: { attributeId: 'hostname', direction: 'asc' }, query: '' },
  isLoadingLayout: false,
  handleColumnPreferencesChange: jest.fn(),
  handleSortChange: jest.fn(),
};

describe('<GraylogNodesExpandable />', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows empty state when no graylog nodes are returned', () => {
    (useClusterGraylogNodesTableLayout as jest.Mock).mockReturnValue(defaultLayout);
    (useClusterGraylogNodes as jest.Mock).mockReturnValue({
      nodes: [],
      total: 0,
      refetch: jest.fn(),
      isLoading: false,
      setPollingEnabled: jest.fn(),
      pollingEnabled: true,
    });

    render(<GraylogNodesExpandable />);

    expect(screen.getByText('No Graylog Nodes found.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('renders entity table with provided nodes', () => {
    (useClusterGraylogNodesTableLayout as jest.Mock).mockReturnValue(defaultLayout);
    const nodes = [
      {
        id: 'graylog-1',
        node_id: 'graylog-1',
        hostname: 'host-1',
        short_node_id: 'g1',
        lifecycle: 'running',
        is_processing: true,
        lb_status: 'ALIVE',
        metrics: {},
      },
    ];

    (useClusterGraylogNodes as jest.Mock).mockReturnValue({
      nodes,
      total: nodes.length,
      refetch: jest.fn(),
      isLoading: false,
      setPollingEnabled: jest.fn(),
      pollingEnabled: true,
    });

    render(<GraylogNodesExpandable />);

    expect(screen.getByRole('table')).toBeInTheDocument();
    expect(screen.getByText(/host-1/i)).toBeInTheDocument();
  });
});
