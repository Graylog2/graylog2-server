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
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';

import FleetReference from './FleetReference';

import { useFleets } from '../hooks';

jest.mock('../hooks/useFleetQueries', () => ({
  ...jest.requireActual('../hooks/useFleetQueries'),
  useFleets: jest.fn(),
}));

const fleet = (id: string, name: string) => ({ id, name, description: '', created_at: '', updated_at: '' });

describe('FleetReference', () => {
  beforeEach(() => {
    asMock(useFleets).mockReturnValue({ data: [fleet('fleet-1', 'web'), fleet('fleet-2', 'db')], isLoading: false });
  });

  it('links to the fleet by name', async () => {
    render(<FleetReference fleetId="fleet-1" />);

    expect(await screen.findByRole('link', { name: 'web' })).toHaveAttribute(
      'href',
      expect.stringContaining('fleet-1'),
    );
  });

  it('renders a removed fleet as text instead of a link', async () => {
    render(<FleetReference fleetId="fleet-gone" />);

    await screen.findByText('Fleet removed (fleet-gone)');

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('links to the target of a pending reassignment', async () => {
    render(<FleetReference fleetId="fleet-1" pendingFleetId="fleet-2" />);

    expect(await screen.findByRole('link', { name: 'db' })).toHaveAttribute('href', expect.stringContaining('fleet-2'));
    await screen.findByText('(moving from web)');
  });

  it('describes a pending move away from a removed fleet', async () => {
    render(<FleetReference fleetId="fleet-gone" pendingFleetId="fleet-2" />);

    await screen.findByRole('link', { name: 'db' });
    await screen.findByText('(moving from a removed fleet)');

    expect(screen.queryByText(/fleet removed/i)).not.toBeInTheDocument();
  });

  it('shows an unreadable pending target by id without claiming it was removed', async () => {
    render(<FleetReference fleetId="fleet-1" pendingFleetId="fleet-other" />);

    await screen.findByText(/fleet-other/);

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.queryByText(/fleet removed/i)).not.toBeInTheDocument();
  });

  it('keeps linking by id while the fleets are loading', async () => {
    asMock(useFleets).mockReturnValue({ data: undefined, isLoading: true });

    render(<FleetReference fleetId="fleet-1" />);

    expect(await screen.findByRole('link', { name: 'fleet-1' })).toBeInTheDocument();
  });
});
