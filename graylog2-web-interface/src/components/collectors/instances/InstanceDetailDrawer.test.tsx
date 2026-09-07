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

import asMock from 'helpers/mocking/AsMock';

import InstanceDetailDrawer from './InstanceDetailDrawer';

import useInstancePendingChanges from '../hooks/useInstancePendingChanges';
import { useInstance } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { CollectorInstanceView, PendingChangesResponse, Source } from '../types';

jest.mock('../hooks/useInstancePendingChanges');
jest.mock('../hooks/useSendCollectorsTelemetry');

jest.mock('../hooks', () => ({
  ...jest.requireActual('../hooks'),
  useInstance: jest.fn(),
}));

const mockInstance: CollectorInstanceView = {
  id: 'inst-1',
  instance_uid: 'uid-1',
  capabilities: 15,
  fleet_id: 'fleet-1',
  enrolled_at: '2026-01-01T00:00:00Z',
  last_seen: new Date().toISOString(),
  active_certificate_fingerprint: 'aa:bb:cc',
  active_certificate_expires_at: '2027-03-17T12:00:00Z',
  next_certificate_fingerprint: null,
  next_certificate_expires_at: null,
  identifying_attributes: { 'host.name': 'prod-web-01' },
  non_identifying_attributes: { 'os.type': 'linux', 'os.description': 'Ubuntu 22.04' },
  hostname: 'prod-web-01',
  os: 'linux',
  version: '1.2.0',
  status: 'online',
  has_pending_changes: false,
  health: null,
};

const mockSources: Source[] = [
  {
    id: 'src-1',
    fleet_id: 'fleet-1',
    name: 'app-logs',
    description: 'Application logs',
    enabled: true,
    type: 'file',
    config: { paths: ['/var/log/app/*.log'], read_mode: 'end' },
  },
];

const pendingChanges: PendingChangesResponse = {
  has_pending_changes: true,
  coalesced: {
    recompute_config: true,
    recompute_ingest_config: false,
    reassign: true,
    restart: false,
    run_discovery: false,
  },
  activities: [
    {
      seq: 42,
      timestamp: '2026-06-10T12:00:00Z',
      type: 'FLEET_REASSIGNED',
      actor: { username: 'alice', full_name: 'Alice Admin' },
      targets: [{ id: 'uid-1', name: 'prod-web-01', type: 'collector' }],
      details: { destination_fleet: { id: 'fleet-2', name: 'Staging', type: 'fleet' } },
    },
  ],
};

describe('InstanceDetailDrawer', () => {
  beforeEach(() => {
    asMock(useSendCollectorsTelemetry).mockReturnValue(jest.fn());
    asMock(useInstancePendingChanges).mockReturnValue({ data: undefined, isLoading: true, isError: false });
    asMock(useInstance).mockReturnValue({ data: undefined, isLoading: true, error: null, isError: false });
  });

  it('renders instance hostname as title', async () => {
    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await screen.findByRole('dialog', { name: /prod-web-01/i });
  });

  it('renders status badge', async () => {
    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await screen.findByText('Online');
  });

  it('renders active sources count', async () => {
    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await screen.findByText(/Active Sources.*1/i);
  });

  it('renders Messages link pointing to agent_id filter', async () => {
    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    const link = await screen.findByRole('link', { name: /^received messages$/i });
    expect(link).toHaveAttribute('href', expect.stringContaining('agent_id'));
    expect(link).toHaveAttribute('href', expect.stringContaining('uid-1'));
  });

  it('renders pending changes as the effects the collector will apply', async () => {
    asMock(useInstancePendingChanges).mockReturnValue({ data: pendingChanges, isLoading: false, isError: false });

    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await screen.findByText('Synchronization');
    await screen.findByText('Sync pending');
    // The effects summary only states that a reassignment is pending, not the destination fleet.
    await screen.findByText(/reassign to another fleet/i);
    await screen.findByText(/reload configuration/i);
    expect(screen.queryByRole('link', { name: 'Staging' })).not.toBeInTheDocument();

    // The queued transactions are collapsed by default and expand on demand. The (permission-filtered)
    // destination fleet is shown there.
    expect(screen.queryByText('by Alice Admin')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /show queued transactions \(1\)/i }));
    await screen.findByText('by Alice Admin');
    await screen.findByRole('link', { name: 'Staging' });
  });

  it('shows a spinner while pending details are loading', async () => {
    asMock(useInstancePendingChanges).mockReturnValue({ data: undefined, isLoading: true, isError: false });
    const pendingInstance = { ...mockInstance, has_pending_changes: true };

    render(
      <InstanceDetailDrawer
        instance={pendingInstance}
        sources={mockSources}
        fleetName="production"
        onClose={jest.fn()}
      />,
    );

    await screen.findByText('Synchronization');
    await screen.findByText(/loading/i);
    expect(screen.queryByText(/queued until the collector synchronizes/i)).not.toBeInTheDocument();
    expect(screen.queryByText('In sync')).not.toBeInTheDocument();
  });

  it('spins while loading instead of asserting a possibly-stale in-sync state', async () => {
    asMock(useInstancePendingChanges).mockReturnValue({ data: undefined, isLoading: true, isError: false });
    // Table row reports in-sync, but the detail hasn't loaded yet; the section must not commit to it.
    const staleInstance = { ...mockInstance, has_pending_changes: false };

    render(
      <InstanceDetailDrawer
        instance={staleInstance}
        sources={mockSources}
        fleetName="production"
        onClose={jest.fn()}
      />,
    );

    await screen.findByText('Synchronization');
    await screen.findByText(/loading/i);
    expect(screen.queryByText(/applied all queued actions/i)).not.toBeInTheDocument();
  });

  it('hides the pending changes section when the instance is caught up', async () => {
    asMock(useInstancePendingChanges).mockReturnValue({
      data: {
        has_pending_changes: false,
        coalesced: {
          recompute_config: false,
          recompute_ingest_config: false,
          reassign: false,
          restart: false,
          run_discovery: false,
        },
        activities: [],
      },
      isLoading: false,
      isError: false,
    });

    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await screen.findByRole('dialog', { name: /prod-web-01/i });
    // "In sync" appears in the top detail row and in the Synchronization section
    expect(await screen.findAllByText('In sync')).toHaveLength(2);
    await screen.findByText(/applied all queued actions/i);
    expect(screen.queryByText(/queued until the collector synchronizes/i)).not.toBeInTheDocument();
  });

  it('shows the instance as pending even when there are no displayable queued actions', async () => {
    // Only UNKNOWN markers are pending: the backend reports has_pending_changes but no effects/activities.
    asMock(useInstancePendingChanges).mockReturnValue({
      data: {
        has_pending_changes: true,
        coalesced: {
          recompute_config: false,
          recompute_ingest_config: false,
          reassign: false,
          restart: false,
          run_discovery: false,
        },
        activities: [],
      },
      isLoading: false,
      isError: false,
    });

    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await screen.findByText('Synchronization');
    // Consistent with the table: pending, not "In sync", with a graceful message rather than an empty list.
    await screen.findByText('Sync pending');
    await screen.findByText(/queued and will be applied/i);
    expect(screen.queryByText('In sync')).not.toBeInTheDocument();
    expect(screen.queryByText(/queued until the collector synchronizes/i)).not.toBeInTheDocument();
  });

  it('shows an error message instead of spinning forever when pending changes fail to load', async () => {
    asMock(useInstancePendingChanges).mockReturnValue({ data: undefined, isLoading: false, isError: true });
    const pendingInstance = { ...mockInstance, has_pending_changes: true };

    render(
      <InstanceDetailDrawer
        instance={pendingInstance}
        sources={mockSources}
        fleetName="production"
        onClose={jest.fn()}
      />,
    );

    await screen.findByText(/could not load pending changes/i);
    expect(screen.queryByText(/loading/i)).not.toBeInTheDocument();
  });

  it('keeps showing cached actions when a background refetch fails', async () => {
    // react-query retains the last good data on a failed refetch but flips isError to true.
    asMock(useInstancePendingChanges).mockReturnValue({ data: pendingChanges, isLoading: false, isError: true });

    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await screen.findByText('Synchronization');
    await screen.findByText(/reload configuration/i); // cached actions still rendered…
    expect(screen.queryByText(/could not load pending changes/i)).not.toBeInTheDocument(); // …not the error arm
  });

  it('leads a bulk reassignment with the instance being viewed', async () => {
    asMock(useInstancePendingChanges).mockReturnValue({
      data: {
        has_pending_changes: true,
        coalesced: {
          recompute_config: false,
          recompute_ingest_config: false,
          reassign: true,
          restart: false,
          run_discovery: false,
        },
        activities: [
          {
            seq: 7,
            timestamp: '2026-06-10T12:00:00Z',
            type: 'FLEET_REASSIGNED',
            actor: null,
            // Bulk marker: 'aaa-other-host' sorts first alphabetically, but we are viewing uid-1.
            targets: [
              { id: 'uid-0', name: 'aaa-other-host', type: 'collector' },
              { id: 'uid-1', name: 'prod-web-01', type: 'collector' },
            ],
            details: { destination_fleet: { id: 'fleet-2', name: 'Staging', type: 'fleet' } },
          },
        ],
      },
      isLoading: false,
      isError: false,
    });

    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await userEvent.click(await screen.findByRole('button', { name: /show queued transactions \(1\)/i }));
    // The viewed instance leads the entry; the other batch member is folded into the count.
    expect(await screen.findByRole('link', { name: 'prod-web-01' })).toBeInTheDocument();
    await screen.findByText(/and 1 other collector/i);
    expect(screen.queryByRole('link', { name: 'aaa-other-host' })).not.toBeInTheDocument();
  });

  it('renders the health section from the instance health', async () => {
    const unhealthy: CollectorInstanceView = {
      ...mockInstance,
      health: {
        healthy_changed_at: '2026-07-31T10:00:00.000+0000',
        component_health: { healthy: false, last_error: 'connection refused' },
      },
    };

    render(
      <InstanceDetailDrawer instance={unhealthy} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await screen.findByText('Health');
    await screen.findByText('Unhealthy');
    await screen.findByText('connection refused');
  });

  it('renders fresh instance data over the stale row snapshot', async () => {
    asMock(useInstance).mockReturnValue({
      data: {
        ...mockInstance,
        status: 'offline',
        health: {
          healthy_changed_at: '2026-07-31T10:00:00.000+0000',
          component_health: { healthy: false, last_error: 'connection refused' },
        },
      },
      isLoading: false,
      error: null,
      isError: false,
    });

    // The stale snapshot says online/no health; the polled data must win.
    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await screen.findByText('Offline');
    await screen.findByText('Last known: Unhealthy');
    await screen.findByText('connection refused');
  });

  it('falls back to the row snapshot while the instance query has no data', async () => {
    asMock(useInstance).mockReturnValue({ data: undefined, isLoading: true, error: null, isError: false });

    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await screen.findByText('Online');
    // Guards the polling wiring: the hook itself handles cadence, session, and error reporting.
    expect(useInstance).toHaveBeenCalledWith('uid-1');
  });

  describe('telemetry', () => {
    const sendTelemetry = jest.fn();

    beforeEach(() => {
      sendTelemetry.mockClear();
      asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    });

    it('reports opening the fleet from the drawer', async () => {
      render(
        <InstanceDetailDrawer
          instance={mockInstance}
          sources={mockSources}
          fleetName="production"
          onClose={jest.fn()}
        />,
      );

      await userEvent.click(await screen.findByRole('link', { name: 'production' }));

      expect(sendTelemetry).toHaveBeenCalledWith(
        'Collector Instance Fleet Opened',
        expect.objectContaining({
          app_action_value: 'instance-drawer-open-fleet',
          instance_id: 'uid-1',
          fleet_id: 'fleet-1',
        }),
      );
    });

    // The same two actions exist as row buttons, so `origin` keeps the surfaces comparable.
    it.each([
      [/view system logs/i, 'Collector Instance View Logs Clicked', 'instance-drawer-view-logs'],
      [/^received messages$/i, 'Collector Instance Received Messages Clicked', 'instance-drawer-received-messages'],
    ])('reports %s from the drawer surface', async (name, eventType, appActionValue) => {
      render(
        <InstanceDetailDrawer
          instance={mockInstance}
          sources={mockSources}
          fleetName="production"
          onClose={jest.fn()}
        />,
      );

      await userEvent.click(await screen.findByRole('link', { name }));

      expect(sendTelemetry).toHaveBeenCalledWith(
        eventType,
        expect.objectContaining({
          app_action_value: appActionValue,
          instance_id: 'uid-1',
          origin: 'detail-drawer',
        }),
      );
    });

    it('reports expanding and collapsing the queued transactions', async () => {
      asMock(useInstancePendingChanges).mockReturnValue({ data: pendingChanges, isLoading: false, isError: false });

      render(
        <InstanceDetailDrawer
          instance={mockInstance}
          sources={mockSources}
          fleetName="production"
          onClose={jest.fn()}
        />,
      );

      await userEvent.click(await screen.findByRole('button', { name: /show queued transactions \(1\)/i }));

      expect(sendTelemetry).toHaveBeenCalledWith(
        'Collector Instance Queued Transactions Toggled',
        expect.objectContaining({
          app_action_value: 'instance-drawer-toggle-transactions',
          shown: true,
          queued_count: 1,
        }),
      );

      await userEvent.click(await screen.findByRole('button', { name: /hide queued transactions/i }));

      expect(sendTelemetry).toHaveBeenCalledWith(
        'Collector Instance Queued Transactions Toggled',
        expect.objectContaining({ shown: false, queued_count: 1 }),
      );
    });
  });
});
