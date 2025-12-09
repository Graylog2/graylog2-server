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

import { PaginatedEntityTable } from 'components/common';

import GraylogNodesExpandable from './GraylogNodesExpandable';

jest.mock('components/common', () => ({
  ...jest.requireActual('components/common'),
  PaginatedEntityTable: jest.fn(({ humanName }) => <div>Paginated {humanName}</div>),
}));

describe('<GraylogNodesExpandable />', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders paginated graylog nodes table', () => {
    render(<GraylogNodesExpandable searchQuery="foo" refetchInterval={10000} />);

    expect(screen.getByText('Paginated Graylog Nodes')).toBeInTheDocument();
    expect(PaginatedEntityTable).toHaveBeenCalledTimes(1);
    const callProps = (PaginatedEntityTable as jest.Mock).mock.calls[0][0];
    expect(callProps.externalSearch.query).toBe('foo');
    expect(callProps.fetchOptions.refetchInterval).toBe(10000);
  });
});
