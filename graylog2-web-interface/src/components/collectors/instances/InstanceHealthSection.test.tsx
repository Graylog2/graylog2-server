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

import InstanceHealthSection from './InstanceHealthSection';

import type { CollectorHealth } from '../types';

const health = (healthy: boolean, lastError?: string): CollectorHealth => ({
  healthy_changed_at: '2026-07-31T10:00:00.000+0000',
  component_health: { healthy, ...(lastError === undefined ? {} : { last_error: lastError }) },
});

describe('InstanceHealthSection', () => {
  it('renders Unknown without duration or error when health was never reported', async () => {
    render(<InstanceHealthSection health={null} online />);

    await screen.findByText('Unknown');
    await screen.findByText(/has not reported health information/i);
    expect(screen.queryByText(/\bfor\b/i)).not.toBeInTheDocument();
  });

  it('renders Unknown even when the instance is offline', async () => {
    render(<InstanceHealthSection health={null} online={false} />);

    await screen.findByText('Unknown');
    expect(screen.queryByText(/last known/i)).not.toBeInTheDocument();
  });

  it('renders Healthy with duration for a healthy online instance', async () => {
    render(<InstanceHealthSection health={health(true)} online />);

    await screen.findByText('Healthy');
    await screen.findByText(/\bfor\b/i);
    expect(screen.queryByText(/for the past/i)).not.toBeInTheDocument();
  });

  it('renders Unhealthy with duration and error for an unhealthy online instance', async () => {
    render(<InstanceHealthSection health={health(false, 'connection refused')} online />);

    await screen.findByText('Unhealthy');
    await screen.findByText(/\bfor\b/i);
    await screen.findByText('connection refused');
  });

  it('renders no error block when unhealthy without error text', async () => {
    render(<InstanceHealthSection health={health(false)} online />);

    await screen.findByText('Unhealthy');
    expect(screen.queryByTestId('health-error')).not.toBeInTheDocument();
  });

  it('renders error text verbatim, escaped', async () => {
    const nasty = 'Get "<http://localhost:13133/health>": context deadline & exceeded';
    render(<InstanceHealthSection health={health(false, nasty)} online />);

    await screen.findByText(nasty);
  });

  it('renders last-known health without a duration for an offline instance', async () => {
    render(<InstanceHealthSection health={health(false, 'connection refused')} online={false} />);

    await screen.findByText('Last known: Unhealthy');
    // The duration would silently include offline time (healthy for 1h, then offline
    // 3 days ≠ "for 3 days"); the Last Seen row above conveys staleness instead.
    expect(screen.queryByText(/\bfor\b/i)).not.toBeInTheDocument();
    // Error stays available for post-mortems even when offline.
    await screen.findByText('connection refused');
  });

  it('renders last-known Healthy for an offline previously-healthy instance', async () => {
    render(<InstanceHealthSection health={health(true)} online={false} />);

    await screen.findByText('Last known: Healthy');
    expect(screen.queryByText(/\bfor\b/i)).not.toBeInTheDocument();
  });

  it('renders no error block when the last known state is healthy', async () => {
    // Defensive: a malformed or stale report may carry last_error alongside
    // healthy: true — a green badge must not be followed by an unexplained error.
    render(<InstanceHealthSection health={health(true, 'stale leftover error')} online />);

    await screen.findByText('Healthy');
    expect(screen.queryByTestId('health-error')).not.toBeInTheDocument();
  });
});
