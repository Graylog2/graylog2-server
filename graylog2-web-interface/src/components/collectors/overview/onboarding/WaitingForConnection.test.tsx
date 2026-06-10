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

import WaitingForConnection from './WaitingForConnection';

jest.mock('components/collectors/hooks/useInstanceQueries', () => ({
  useInstances: jest.fn(),
}));

const instance = (id: string, enrolledAt: string): CollectorInstanceView =>
  ({
    id,
    instance_uid: id,
    fleet_id: 'fleet-1',
    enrolled_at: enrolledAt,
    last_seen: enrolledAt,
    status: 'online',
    identifying_attributes: {},
    non_identifying_attributes: {},
    hostname: `host-${id}`,
    os: 'linux',
    version: '1.0.0',
  }) as CollectorInstanceView;

describe('WaitingForConnection', () => {
  const onConnected = jest.fn();

  const mockInstances = (data: CollectorInstanceView[] | undefined, error: Error | null = null) =>
    asMock(useInstances).mockReturnValue({ data, error } as ReturnType<typeof useInstances>);

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows the waiting status', () => {
    mockInstances([]);

    render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(screen.getByText(/waiting for connection/i)).toBeInTheDocument();
  });

  it('polls instances for the selected fleet silently (no toast on transient failures)', () => {
    mockInstances([]);

    render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(useInstances).toHaveBeenCalledWith('fleet-1', { refetchInterval: 3000, silent: true });
  });

  it('shows an inline notice when polling fails, and keeps waiting', () => {
    mockInstances(undefined, new Error('nope'));

    render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(screen.getByText(/having trouble reaching the server/i)).toBeInTheDocument();
    expect(screen.getByText(/waiting for connection/i)).toBeInTheDocument();
    expect(onConnected).not.toHaveBeenCalled();
  });

  it('does not fire for instances that existed before onboarding', () => {
    mockInstances([instance('pre-existing', '2026-06-10T10:00:00Z')]);

    render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(onConnected).not.toHaveBeenCalled();
  });

  it('fires once a new instance appears after the baseline poll', () => {
    mockInstances([instance('pre-existing', '2026-06-10T10:00:00Z')]);

    const { rerender } = render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    mockInstances([instance('pre-existing', '2026-06-10T10:00:00Z'), instance('fresh', '2026-06-10T12:00:00Z')]);

    rerender(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(onConnected).toHaveBeenCalledTimes(1);
    expect(onConnected).toHaveBeenCalledWith(expect.objectContaining({ id: 'fresh' }));
  });

  it('picks the earliest enrolled instance when several appear', () => {
    mockInstances([]);

    const { rerender } = render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    mockInstances([instance('later', '2026-06-10T12:05:00Z'), instance('earlier', '2026-06-10T12:01:00Z')]);

    rerender(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(onConnected).toHaveBeenCalledTimes(1);
    expect(onConnected).toHaveBeenCalledWith(expect.objectContaining({ id: 'earlier' }));
  });

  it('fires at most once', () => {
    mockInstances([]);

    const { rerender } = render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    mockInstances([instance('a', '2026-06-10T12:00:00Z')]);

    rerender(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    mockInstances([instance('a', '2026-06-10T12:00:00Z'), instance('b', '2026-06-10T12:01:00Z')]);

    rerender(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(onConnected).toHaveBeenCalledTimes(1);
  });
});
