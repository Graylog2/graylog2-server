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
  instance_uid: 'uid-1',
  capabilities: 15,
  fleet_id: 'fleet-1',
  enrolled_at: '2026-01-01T00:00:00Z',
  last_seen: new Date().toISOString(),
  certificate_fingerprint: 'aa:bb:cc',
  identifying_attributes: { 'host.name': 'prod-web-01' },
  non_identifying_attributes: { 'os.type': 'linux', 'os.description': 'Ubuntu 22.04' },
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
