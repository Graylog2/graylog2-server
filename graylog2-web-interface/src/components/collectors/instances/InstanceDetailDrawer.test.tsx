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

import InstanceDetailDrawer from './InstanceDetailDrawer';

import type { CollectorInstanceView, Source } from '../types';


const mockInstance: CollectorInstanceView = {
  id: 'inst-1',
  agent_id: 'agent-1',
  instance_uid: 'uid-1',
  agent_description: {
    identifying_attributes: [{ key: 'host.name', value: 'prod-web-01' }],
    non_identifying_attributes: [
      { key: 'os.type', value: 'linux' },
      { key: 'os.description', value: 'Ubuntu 22.04' },
    ],
  },
  remote_config_status: { last_remote_config_hash: 'abc123', status: 'APPLIED', error_message: null },
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

describe('InstanceDetailDrawer', () => {
  it('renders instance hostname as title', async () => {
    render(
      <InstanceDetailDrawer
        instance={mockInstance}
        sources={mockSources}
        fleetName="production"
        onClose={jest.fn()}
      />,
    );

    await screen.findByRole('dialog', { name: /prod-web-01/i });
  });

  it('renders status badge', async () => {
    render(
      <InstanceDetailDrawer
        instance={mockInstance}
        sources={mockSources}
        fleetName="production"
        onClose={jest.fn()}
      />,
    );

    await screen.findByText('Online');
  });

  it('renders config status', async () => {
    render(
      <InstanceDetailDrawer
        instance={mockInstance}
        sources={mockSources}
        fleetName="production"
        onClose={jest.fn()}
      />,
    );

    await screen.findByText('APPLIED');
  });

  it('renders active sources count', async () => {
    render(
      <InstanceDetailDrawer
        instance={mockInstance}
        sources={mockSources}
        fleetName="production"
        onClose={jest.fn()}
      />,
    );

    await screen.findByText(/Active Sources.*1/i);
  });
});
