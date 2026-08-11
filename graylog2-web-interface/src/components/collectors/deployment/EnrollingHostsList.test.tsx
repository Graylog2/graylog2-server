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
import { useInstances } from 'components/collectors/hooks/useInstanceQueries';
import type { CollectorInstanceView } from 'components/collectors/types';

import EnrollingHostsList from './EnrollingHostsList';

jest.mock('components/collectors/hooks/useInstanceQueries', () => ({
  useInstances: jest.fn(),
}));

const instance = (id: string, hostname: string, enrolledAt: string): CollectorInstanceView =>
  ({
    id,
    instance_uid: `uid-${id}`,
    fleet_id: 'fleet-1',
    enrolled_at: enrolledAt,
    last_seen: enrolledAt,
    status: 'online',
    identifying_attributes: {},
    non_identifying_attributes: {},
    hostname,
    os: 'linux',
    version: '1.0.0',
  }) as CollectorInstanceView;

describe('EnrollingHostsList', () => {
  const defaultProps = { fleetId: 'fleet-1', fleetName: 'web-servers', platformId: 'linux' as const };

  const mockInstances = (data: CollectorInstanceView[] | undefined, error: Error | null = null) =>
    asMock(useInstances).mockReturnValue({ data, error } as ReturnType<typeof useInstances>);

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('polls instances for the fleet silently', () => {
    mockInstances([]);

    render(<EnrollingHostsList {...defaultProps} />);

    expect(useInstances).toHaveBeenCalledWith('fleet-1', { refetchInterval: 3000, silent: true });
  });

  it('shows a listening hint and no rows for pre-existing instances', () => {
    mockInstances([instance('pre-existing', 'old-host', '2026-06-10T10:00:00Z')]);

    render(<EnrollingHostsList {...defaultProps} />);

    expect(screen.getByText(/hosts running the command appear here/i)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'old-host' })).not.toBeInTheDocument();
  });

  it('lists hosts that appear after the baseline, linking to the onboarding detail', () => {
    mockInstances([]);

    const { rerender } = render(<EnrollingHostsList {...defaultProps} />);

    mockInstances([instance('fresh', 'web-prod-02', '2026-08-11T10:00:00Z')]);

    rerender(<EnrollingHostsList {...defaultProps} />);

    const hostLink = screen.getByRole('link', { name: 'web-prod-02' });

    expect(hostLink).toHaveAttribute('href', expect.stringContaining('uid-fresh'));
    expect(screen.getByText(/1 connected/i)).toBeInTheDocument();
  });

  it('orders enrolling hosts newest first', () => {
    mockInstances([]);

    const { rerender } = render(<EnrollingHostsList {...defaultProps} />);

    mockInstances([
      instance('older', 'host-older', '2026-08-11T10:00:00Z'),
      instance('newer', 'host-newer', '2026-08-11T11:00:00Z'),
    ]);

    rerender(<EnrollingHostsList {...defaultProps} />);

    const links = screen.getAllByRole('link', { name: /host-/ });

    expect(links.map((link) => link.textContent)).toEqual(['host-newer', 'host-older']);
  });

  it('shows an inline notice when polling fails, and keeps listening', () => {
    mockInstances(undefined, new Error('nope'));

    render(<EnrollingHostsList {...defaultProps} />);

    expect(screen.getByText(/having trouble reaching the server/i)).toBeInTheDocument();
  });
});
