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

import MongodbNodesExpandable from './MongodbNodesExpandable';
import { clusterMongodbNodesKeyFn, fetchMongodbNodes } from './fetchClusterMongodbNodes';

jest.mock('components/common/PaginatedEntityTable', () => ({
  __esModule: true,
  default: jest.fn(({ humanName }) => <div>Paginated {humanName}</div>),
  useTableFetchContext: jest.fn(),
}));

jest.mock('./MongodbProfilingAction', () => ({
  __esModule: true,
  default: jest.fn(() => <div>MongoDB Profiling Action</div>),
}));

describe('<MongodbNodesExpandable />', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders paginated entity table with proper props', () => {
    const { default: PaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const { default: MongodbProfilingAction } = jest.requireMock('./MongodbProfilingAction');
    const mockPaginatedEntityTable = asMock(PaginatedEntityTable);
    const mockMongodbProfilingAction = asMock(MongodbProfilingAction);

    render(<MongodbNodesExpandable searchQuery="role:secondary" refetchInterval={10000} />);

    expect(screen.getByText('Paginated MongoDB Nodes')).toBeInTheDocument();
    expect(screen.getByText('MongoDB Profiling Action')).toBeInTheDocument();
    expect(mockMongodbProfilingAction.mock.calls[0][0]).toEqual({});
    expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(1);
    const callProps = mockPaginatedEntityTable.mock.calls[0][0] as PaginatedEntityTableProps<any, any>;
    expect(callProps.humanName).toBe('MongoDB Nodes');
    expect(callProps.fetchEntities).toBe(fetchMongodbNodes);
    expect(callProps.keyFn).toBe(clusterMongodbNodesKeyFn);
    expect(callProps.externalSearch.query).toBe('role:secondary');
    expect(callProps.fetchOptions.refetchInterval).toBe(10000);
  });
});
