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
  it('renders the interpretation legend by default when the synthetic root is selected', () => {
    render(<HealthModule />);

    expect(screen.getByRole('heading', { name: 'Health of Graylog Deployment' })).toBeInTheDocument();
    expect(screen.getByText('Cluster Health')).toBeInTheDocument();
    expect(screen.getByLabelText('Cluster health tree')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'How to interpret this health report:' })).toBeInTheDocument();

    expect(screen.getByText(/The feature is functioning properly/)).toBeInTheDocument();
    expect(screen.getByText(/The feature is experiencing a problem/)).toBeInTheDocument();
    expect(screen.getByText(/The feature has severe issues/)).toBeInTheDocument();
    expect(screen.getByText(/The state could not be evaluated/)).toBeInTheDocument();
  });

  it('renders Affected list with non-healthy children when a feature is selected', async () => {
    render(<HealthModule />);

    await userEvent.click(screen.getByText('MongoDB'));

    expect(screen.getByRole('heading', { name: 'MongoDB' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Affected' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Connectivity/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Primary State/i })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'How to interpret this health report:' })).not.toBeInTheDocument();
  });

  it('renders the full check panel when a non-healthy leaf is selected', async () => {
    render(<HealthModule />);

    await userEvent.click(screen.getByText('MongoDB'));
    await userEvent.click(screen.getByText('Primary State'));

    expect(screen.getByRole('heading', { name: 'Primary State' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'What this means' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Common causes' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Recommended action' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Affected' })).not.toBeInTheDocument();
  });

  it('renders the entity-list button with the configured label', async () => {
    render(<HealthModule />);

    await userEvent.click(screen.getByText('MongoDB'));
    await userEvent.click(screen.getByText('Connectivity'));

    expect(screen.getByRole('link', { name: /View MongoDB/i })).toBeInTheDocument();
  });
});
