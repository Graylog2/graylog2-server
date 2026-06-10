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

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows the waiting status', () => {
    asMock(useInstances).mockReturnValue({ data: [] } as ReturnType<typeof useInstances>);

    render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(screen.getByText(/waiting for connection/i)).toBeInTheDocument();
  });

  it('polls instances for the selected fleet', () => {
    asMock(useInstances).mockReturnValue({ data: [] } as ReturnType<typeof useInstances>);

    render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(useInstances).toHaveBeenCalledWith('fleet-1', { refetchInterval: 3000 });
  });

  it('does not fire for instances that existed before onboarding', () => {
    asMock(useInstances).mockReturnValue({
      data: [instance('pre-existing', '2026-06-10T10:00:00Z')],
    } as ReturnType<typeof useInstances>);

    render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(onConnected).not.toHaveBeenCalled();
  });

  it('fires once a new instance appears after the baseline poll', () => {
    asMock(useInstances).mockReturnValue({
      data: [instance('pre-existing', '2026-06-10T10:00:00Z')],
    } as ReturnType<typeof useInstances>);

    const { rerender } = render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    asMock(useInstances).mockReturnValue({
      data: [instance('pre-existing', '2026-06-10T10:00:00Z'), instance('fresh', '2026-06-10T12:00:00Z')],
    } as ReturnType<typeof useInstances>);

    rerender(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(onConnected).toHaveBeenCalledTimes(1);
    expect(onConnected).toHaveBeenCalledWith(expect.objectContaining({ id: 'fresh' }));
  });

  it('picks the earliest enrolled instance when several appear', () => {
    asMock(useInstances).mockReturnValue({ data: [] } as ReturnType<typeof useInstances>);

    const { rerender } = render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    asMock(useInstances).mockReturnValue({
      data: [instance('later', '2026-06-10T12:05:00Z'), instance('earlier', '2026-06-10T12:01:00Z')],
    } as ReturnType<typeof useInstances>);

    rerender(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(onConnected).toHaveBeenCalledTimes(1);
    expect(onConnected).toHaveBeenCalledWith(expect.objectContaining({ id: 'earlier' }));
  });

  it('fires at most once', () => {
    asMock(useInstances).mockReturnValue({ data: [] } as ReturnType<typeof useInstances>);

    const { rerender } = render(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    asMock(useInstances).mockReturnValue({
      data: [instance('a', '2026-06-10T12:00:00Z')],
    } as ReturnType<typeof useInstances>);

    rerender(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    asMock(useInstances).mockReturnValue({
      data: [instance('a', '2026-06-10T12:00:00Z'), instance('b', '2026-06-10T12:01:00Z')],
    } as ReturnType<typeof useInstances>);

    rerender(<WaitingForConnection fleetId="fleet-1" onConnected={onConnected} />);

    expect(onConnected).toHaveBeenCalledTimes(1);
  });
});
