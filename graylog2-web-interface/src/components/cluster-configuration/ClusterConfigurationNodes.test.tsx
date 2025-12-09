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
import { PaginatedEntityTable } from 'components/common';

import ClusterConfigurationNodes from './ClusterConfigurationNodes';

jest.mock('components/common', () => ({
  ...jest.requireActual('components/common'),
  PaginatedEntityTable: jest.fn(() => <div role="table">paginated-table</div>),
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
    render(<ClusterConfigurationNodes />);

    expect(screen.getAllByRole('table')).toHaveLength(2);
    expect(PaginatedEntityTable).toHaveBeenCalledTimes(2);
  });

  it('switches to a specific node type when segmented control is used', async () => {
    render(<ClusterConfigurationNodes />);

    await userEvent.click(screen.getByRole('radio', { name: 'Data Nodes' }));

    await waitFor(() => expect(PaginatedEntityTable).toHaveBeenCalledTimes(1));
  });

  it('uses child "select node type" handler to switch view', async () => {
    render(<ClusterConfigurationNodes />);

    await userEvent.click(screen.getByRole('radio', { name: 'Data Nodes' }));

    await waitFor(() => expect(PaginatedEntityTable).toHaveBeenCalledTimes(1));
  });

  it('passes trimmed search query to children', async () => {
    render(<ClusterConfigurationNodes />);

    const searchInput = screen.getByPlaceholderText('Search nodes…');

    await userEvent.type(searchInput, '  nodes  ');
    act(() => {
      jest.advanceTimersByTime(SEARCH_DEBOUNCE_THRESHOLD + 10);
    });

    await waitFor(() => expect(PaginatedEntityTable).toHaveBeenCalled());
  });
});
