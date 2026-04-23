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
import * as React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from 'wrappedTestingLibrary';

import HealthModule from './HealthModule';

describe('HealthModule', () => {
  it('renders the interpretation legend by default when the root node is selected', () => {
    render(<HealthModule />);

    expect(screen.queryByText('Preview')).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Health of Graylog Deployment' })).toBeInTheDocument();
    expect(screen.getByLabelText('Cluster health tree')).toBeInTheDocument();
    expect(screen.getByText('Cluster Health')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Output Failures' })).not.toBeInTheDocument();

    expect(screen.getByRole('heading', { name: 'How to interpret this health report:' })).toBeInTheDocument();
    expect(screen.getByText(/Operating within the expected band/)).toBeInTheDocument();
    expect(screen.getByText(/Requires attention before it turns into an outage/)).toBeInTheDocument();
    expect(screen.getByText(/Actively impacting the cluster and needs intervention/)).toBeInTheDocument();
    expect(screen.getByText(/Not currently evaluated or intentionally turned off/)).toBeInTheDocument();
  });

  it('updates the detail panel when a tree node is selected', async () => {
    render(<HealthModule />);

    await userEvent.click(screen.getByText('MongoDB'));

    expect(screen.getByRole('heading', { name: 'MongoDB' })).toBeInTheDocument();
    expect(screen.getByText('Platform dependency checks for MongoDB connectivity, topology, and storage behavior.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Connectivity 1 nested check/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Primary State 1 nested check/i })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'How to interpret this health report:' })).not.toBeInTheDocument();
  });
});
