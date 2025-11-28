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

import DataNodesExpandable from './DataNodesExpandable';
import useClusterDataNodes from './useClusterDataNodes';
import useClusterDataNodesTableLayout from './useClusterDataNodesTableLayout';

jest.mock('./useClusterDataNodes');
jest.mock('./useClusterDataNodesTableLayout');

const defaultLayout = {
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

describe('<DataNodesExpandable />', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows empty state when no data nodes are returned', () => {
    (useClusterDataNodesTableLayout as jest.Mock).mockReturnValue(defaultLayout);
    (useClusterDataNodes as jest.Mock).mockReturnValue({
      nodes: [],
      total: 0,
      refetch: jest.fn(),
      isLoading: false,
      setPollingEnabled: jest.fn(),
      pollingEnabled: true,
    });

    render(<DataNodesExpandable />);

    expect(screen.getByText('No Data Nodes found.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('renders entity table with provided nodes', () => {
    (useClusterDataNodesTableLayout as jest.Mock).mockReturnValue(defaultLayout);
    const nodes = [
      {
        id: 'node-1',
        node_id: 'node-1',
        hostname: 'host-1',
        datanode_status: 'AVAILABLE',
        datanode_version: '1.0',
        opensearch_roles: ['data'],
        metrics: {},
      },
    ];

    (useClusterDataNodes as jest.Mock).mockReturnValue({
      nodes,
      total: nodes.length,
      refetch: jest.fn(),
      isLoading: false,
      setPollingEnabled: jest.fn(),
      pollingEnabled: true,
    });

    render(<DataNodesExpandable />);

    expect(screen.getByRole('table')).toBeInTheDocument();
    expect(screen.getByText('host-1')).toBeInTheDocument();
  });
});
