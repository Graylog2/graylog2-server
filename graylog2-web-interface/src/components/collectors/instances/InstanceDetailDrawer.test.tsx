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
  coalesced: {
    recompute_config: true,
    recompute_ingest_config: false,
    reassign_target_fleet_id: 'fleet-2',
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
    asMock(useInstancePendingChanges).mockReturnValue({ data: undefined, isLoading: true });
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

  it('renders pending changes with coalesced summary and activity entries', async () => {
    asMock(useInstancePendingChanges).mockReturnValue({ data: pendingChanges, isLoading: false });

    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await screen.findByText('Pending changes');
    await screen.findByText('Reassign to Staging');
    await screen.findByText('Configuration update');
    await screen.findByText(/reassigned/i);
    await screen.findByText('by Alice Admin');
  });

  it('hides the pending changes section when the instance is caught up', async () => {
    asMock(useInstancePendingChanges).mockReturnValue({
      data: {
        coalesced: {
          recompute_config: false,
          recompute_ingest_config: false,
          reassign_target_fleet_id: null,
          restart: false,
          run_discovery: false,
        },
        activities: [],
      },
      isLoading: false,
    });

    render(
      <InstanceDetailDrawer instance={mockInstance} sources={mockSources} fleetName="production" onClose={jest.fn()} />,
    );

    await screen.findByRole('dialog', { name: /prod-web-01/i });
    expect(screen.queryByText('Pending changes')).not.toBeInTheDocument();
  });
});
