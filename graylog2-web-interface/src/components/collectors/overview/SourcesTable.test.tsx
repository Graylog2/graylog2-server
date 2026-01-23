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

import SourcesTable from './SourcesTable';

import type { Source } from '../types';


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

describe('SourcesTable', () => {
  it('renders source name and type', async () => {
    render(<SourcesTable sources={mockSources} fleetNames={{ 'fleet-1': 'production' }} />);

    await screen.findByText('app-logs');
    await screen.findByText('File');
  });

  it('renders fleet name', async () => {
    render(<SourcesTable sources={mockSources} fleetNames={{ 'fleet-1': 'production' }} />);

    await screen.findByText('production');
  });

  it('renders empty state when no sources', async () => {
    render(<SourcesTable sources={[]} fleetNames={{}} />);

    await screen.findByText(/no sources configured/i);
  });
});
