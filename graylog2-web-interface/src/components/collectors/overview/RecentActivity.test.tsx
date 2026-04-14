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

import RecentActivity from './RecentActivity';

import { useRecentActivity } from '../hooks';
import type { RecentActivityResponse, TargetInfo } from '../types';

jest.mock('../hooks', () => ({
  ...jest.requireActual('../hooks'),
  useRecentActivity: jest.fn(),
}));

const fleetTarget = (id: string, name: string): TargetInfo => ({ id, name, type: 'fleet' });

const activityResponse = (targets: TargetInfo[]): { data: RecentActivityResponse; isLoading: boolean } => ({
  isLoading: false,
  data: {
    activities: [{
      seq: 1,
      timestamp: '2026-04-10T08:00:00Z',
      type: 'CONFIG_CHANGED',
      actor: null,
      targets,
    }],
  },
});

describe('RecentActivity', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows single fleet target without additional text', async () => {
    asMock(useRecentActivity).mockReturnValue(
      activityResponse([fleetTarget('f1', 'Alpha Fleet')]),
    );

    render(<RecentActivity />);

    await screen.findByText('Alpha Fleet');

    expect(screen.queryByText(/and \d+ other/)).not.toBeInTheDocument();
  });

  it('shows "and 1 other fleet" for two targets', async () => {
    asMock(useRecentActivity).mockReturnValue(
      activityResponse([fleetTarget('f1', 'Alpha Fleet'), fleetTarget('f2', 'Beta Fleet')]),
    );

    render(<RecentActivity />);

    await screen.findByText('Alpha Fleet');
    await screen.findByText(/and 1 other fleet/);
  });

  it('shows "and N other fleets" for three or more targets', async () => {
    asMock(useRecentActivity).mockReturnValue(
      activityResponse([
        fleetTarget('f1', 'Alpha Fleet'),
        fleetTarget('f2', 'Beta Fleet'),
        fleetTarget('f3', 'Gamma Fleet'),
      ]),
    );

    render(<RecentActivity />);

    await screen.findByText('Alpha Fleet');
    await screen.findByText(/and 2 other fleets/);
  });

  it('sorts targets alphabetically and displays the first', async () => {
    asMock(useRecentActivity).mockReturnValue(
      activityResponse([
        fleetTarget('f2', 'Zulu Fleet'),
        fleetTarget('f1', 'Alpha Fleet'),
      ]),
    );

    render(<RecentActivity />);

    const link = await screen.findByRole('link', { name: 'Alpha Fleet' });

    expect(link).toBeInTheDocument();
  });

  it('does not mutate the original targets array', () => {
    const targets: TargetInfo[] = [
      fleetTarget('f2', 'Zulu Fleet'),
      fleetTarget('f1', 'Alpha Fleet'),
    ];
    const originalOrder = [...targets];

    asMock(useRecentActivity).mockReturnValue(activityResponse(targets));

    render(<RecentActivity />);

    expect(targets).toEqual(originalOrder);
  });
});
