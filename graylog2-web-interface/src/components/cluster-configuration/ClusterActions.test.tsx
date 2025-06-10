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
import userEvent from '@testing-library/user-event';

import type { GraylogNode } from './useClusterNodes';
import ClusterActions from './ClusterActions';

const nodeMock: GraylogNode = {
  cluster_id: '600120f7-a111-4ec3-88b0-aee4067fb21d',
  type: 'server',
  codename: 'Noir',
  facility: 'graylog-server',
  hostname: 'localhost',
  is_leader: true,
  is_processing: true,
  last_seen: '2025-03-06T09:36:14.000Z',
  lb_status: 'alive',
  lifecycle: 'running',
  node_id: 'a853111d-85ce-42c0-bc6d-22f7f0fecbb4',
  operating_system: 'Mac OS X 15.3.1',
  short_node_id: 'a853553d',
  started_at: '2025-03-04T14:17:26.310Z',
  timezone: 'Europe/Berlin',
  transport_address: 'http://192.168.64.1:9000/api/',
  version: '6.2.0-SNAPSHOT',
};

describe('ClusterActions', () => {
  it('opens upon click on More button', async () => {
    render(<ClusterActions node={nodeMock} />);

    const button = await screen.findByRole('button', { name: /More/i });
    await userEvent.click(button);

    await screen.findByRole('menuitem', { name: /Metrics/i });
    await screen.findByRole('menuitem', { name: /API Browser/i });
  });

  it('shows pause message processing menu item when is_processing true', async () => {
    render(<ClusterActions node={{ ...nodeMock, is_processing: true }} />);

    const button = await screen.findByRole('button', { name: /More/i });
    await userEvent.click(button);

    await screen.findByRole('menuitem', { name: /Pause message processing/i });
  });

  it('shows resume message processing menu item when is_processing false', async () => {
    render(<ClusterActions node={{ ...nodeMock, is_processing: false }} />);

    const button = await screen.findByRole('button', { name: /More/i });
    await userEvent.click(button);

    await screen.findByRole('menuitem', { name: /Resume message processing/i });
  });

  it('shows set load balancer to DEAD menu item when lb_status is alive', async () => {
    render(<ClusterActions node={{ ...nodeMock, lb_status: 'alive' }} />);

    const button = await screen.findByRole('button', { name: /More/i });
    await userEvent.click(button);

    await screen.findByRole('menuitem', { name: /Override load Balancer status to DEAD/i });
  });

  it('shows set load balancer to ALIVE menu item when lb_status is not alive', async () => {
    render(<ClusterActions node={{ ...nodeMock, lb_status: 'dead' }} />);

    const button = await screen.findByRole('button', { name: /More/i });
    await userEvent.click(button);

    await screen.findByRole('menuitem', { name: /Override load Balancer status to ALIVE/i });
  });
});
