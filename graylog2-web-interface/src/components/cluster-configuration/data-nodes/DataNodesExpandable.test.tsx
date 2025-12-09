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

import asMock from 'helpers/mocking/AsMock';
import type { PaginatedEntityTableProps } from 'components/common/PaginatedEntityTable/PaginatedEntityTable';

import DataNodesExpandable from './DataNodesExpandable';
import { clusterDataNodesKeyFn, fetchClusterDataNodesWithMetrics } from './fetchClusterDataNodes';

jest.mock('components/common/PaginatedEntityTable', () => ({
  __esModule: true,
  default: jest.fn(({ humanName }) => <div>Paginated {humanName}</div>),
  useTableFetchContext: jest.fn(),
}));

describe('<DataNodesExpandable />', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders paginated entity table with proper props', () => {
    const { default: PaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(PaginatedEntityTable);

    render(<DataNodesExpandable searchQuery="status:up" refetchInterval={10000} />);

    expect(screen.getByText('Paginated Data Nodes')).toBeInTheDocument();
    expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(1);
    const callProps = mockPaginatedEntityTable.mock.calls[0][0] as PaginatedEntityTableProps<any, any>;
    expect(callProps.humanName).toBe('Data Nodes');
    expect(callProps.fetchEntities).toBe(fetchClusterDataNodesWithMetrics);
    expect(callProps.keyFn).toBe(clusterDataNodesKeyFn);
    expect(callProps.externalSearch.query).toBe('status:up');
    expect(callProps.fetchOptions.refetchInterval).toBe(10000);
  });
});
