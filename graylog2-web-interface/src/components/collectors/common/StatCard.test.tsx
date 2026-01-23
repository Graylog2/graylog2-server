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

import StatCard from './StatCard';

describe('StatCard', () => {
  it('renders value and label', async () => {
    render(<StatCard value={42} label="Instances" />);

    await screen.findByText('42');
    await screen.findByText('Instances');
  });

  it('applies variant styling for warning', async () => {
    render(<StatCard value={5} label="Offline" variant="warning" />);

    const card = await screen.findByTestId('stat-card');
    expect(card).toBeInTheDocument();
  });
});
