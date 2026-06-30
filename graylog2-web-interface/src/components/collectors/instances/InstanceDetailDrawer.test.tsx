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
import type { CollectorInstanceView, PendingChangesResponse, Source } from '../types';

jest.mock('../hooks/useInstancePendingChanges');

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
    asMock(useInstancePendingChanges).mockReturnValue({ data: undefined, isLoading: true, isError: false });
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

  it('renders Messages link pointing to collector_instance_uid filter', async () => {
    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    const link = await screen.findByRole('link', { name: /^received messages$/i });
    expect(link).toHaveAttribute('href', expect.stringContaining('collector_instance_uid'));
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
      <InstanceDetailDrawer instance={staleInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
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
});
