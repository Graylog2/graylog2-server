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

import FleetList from './FleetList';

import type { Fleet } from '../types';


const mockFleets: Fleet[] = [
  {
    id: 'fleet-1',
    name: 'production',
    description: 'Production servers',
    target_version: '1.2.0',
    created_at: '2026-01-01T00:00:00Z',
    updated_at: '2026-01-20T00:00:00Z',
  },
];

describe('FleetList', () => {
  it('renders fleet name and description', async () => {
    render(<FleetList fleets={mockFleets} />);

    await screen.findByText('production');
    await screen.findByText('Production servers');
  });

  it('renders empty state when no fleets', async () => {
    render(<FleetList fleets={[]} />);

    await screen.findByText(/no fleets/i);
  });
});
