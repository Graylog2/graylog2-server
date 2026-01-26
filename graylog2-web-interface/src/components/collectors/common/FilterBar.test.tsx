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
import userEvent from '@testing-library/user-event';

import FilterBar from './FilterBar';

describe('FilterBar', () => {
  const defaultProps = {
    searchValue: '',
    onSearchChange: jest.fn(),
    statusFilter: 'all' as const,
    onStatusFilterChange: jest.fn(),
    fleetFilter: null as string | null,
    onFleetFilterChange: jest.fn(),
    fleetOptions: [
      { value: 'fleet-1', label: 'production' },
      { value: 'fleet-2', label: 'staging' },
    ],
  };

  it('renders search input', async () => {
    render(<FilterBar {...defaultProps} />);

    await screen.findByPlaceholderText(/filter/i);
  });

  it('renders status filter', async () => {
    render(<FilterBar {...defaultProps} />);

    await screen.findByText('All');
    await screen.findByText('Online');
    await screen.findByText('Offline');
  });

  it('calls onSearchChange when typing', async () => {
    const onSearchChange = jest.fn();
    render(<FilterBar {...defaultProps} onSearchChange={onSearchChange} />);

    const input = await screen.findByPlaceholderText(/filter/i);
    await userEvent.type(input, 'test');

    expect(onSearchChange).toHaveBeenCalled();
  });
});
