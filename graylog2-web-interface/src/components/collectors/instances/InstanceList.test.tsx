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

import type { CollectorInstanceView } from '../types';

import InstanceList from './InstanceList';

const mockInstances: CollectorInstanceView[] = [
  {
    id: 'inst-1',
    agent_id: 'agent-1',
    instance_uid: 'uid-1',
    agent_description: {
      identifying_attributes: [{ key: 'host.name', value: 'prod-web-01' }],
      non_identifying_attributes: [{ key: 'os.type', value: 'linux' }],
    },
    remote_config_status: { last_remote_config_hash: 'abc', status: 'APPLIED', error_message: null },
    health: { healthy: true, start_time_unix_nano: 0, last_error: null },
    capabilities: 15,
    fleet_id: 'fleet-1',
    first_seen: '2026-01-01T00:00:00Z',
    last_seen: new Date().toISOString(),
    connection_type: 'WEBSOCKET',
    hostname: 'prod-web-01',
    os: 'linux',
    version: '1.2.0',
    status: 'online',
  },
];

describe('InstanceList', () => {
  it('renders instance hostname', async () => {
    render(<InstanceList instances={mockInstances} fleetNames={{ 'fleet-1': 'production' }} />);

    await screen.findByText('prod-web-01');
  });

  it('renders online status badge', async () => {
    render(<InstanceList instances={mockInstances} fleetNames={{ 'fleet-1': 'production' }} />);

    // The Badge has the mantine-Badge-label class
    const badge = await screen.findByRole('table');
    expect(badge).toHaveTextContent('Online');
  });

  it('renders empty state when no instances', async () => {
    render(<InstanceList instances={[]} fleetNames={{}} />);

    await screen.findByText(/no instances/i);
  });
});
