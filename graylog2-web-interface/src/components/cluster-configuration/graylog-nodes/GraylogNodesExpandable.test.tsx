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

import type { PaginatedEntityTableProps } from 'components/common/PaginatedEntityTable/PaginatedEntityTable';
import asMock from 'helpers/mocking/AsMock';

import GraylogNodesExpandable from './GraylogNodesExpandable';

jest.mock('components/common/PaginatedEntityTable', () => ({
  __esModule: true,
  default: jest.fn(({ humanName }) => <div>Paginated {humanName}</div>),
  useTableFetchContext: jest.fn(),
}));
jest.mock('brand-customization/useProductName', () => jest.fn(() => 'Graylog'));

describe('<GraylogNodesExpandable />', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders paginated graylog nodes table', () => {
    const { default: MockPaginatedEntityTable } = jest.requireMock('components/common/PaginatedEntityTable');
    const mockPaginatedEntityTable = asMock(MockPaginatedEntityTable);

    render(<GraylogNodesExpandable searchQuery="foo" refetchInterval={10000} />);

    expect(screen.getByText('Paginated Graylog Nodes')).toBeInTheDocument();
    expect(mockPaginatedEntityTable).toHaveBeenCalledTimes(1);
    const callProps = mockPaginatedEntityTable.mock.calls[0][0] as PaginatedEntityTableProps<any, any>;
    expect(callProps.humanName).toBe('Graylog Nodes');
    expect(callProps.externalSearch.query).toBe('foo');
    expect(callProps.fetchOptions.refetchInterval).toBe(10000);
  });
});
