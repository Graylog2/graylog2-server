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

import asMock from 'helpers/mocking/AsMock';
import { useSources } from 'components/collectors/hooks/useSourceQueries';
import type { CollectorInstanceView, Source } from 'components/collectors/types';

import ConnectionSuccess from './ConnectionSuccess';
import useCollectorLogPreview from './useCollectorLogPreview';

jest.mock('./useCollectorLogPreview');
jest.mock('components/collectors/hooks/useSourceQueries', () => ({
  useSources: jest.fn(),
}));

const instance = {
  id: 'uid-42',
  instance_uid: 'uid-42',
  fleet_id: 'fleet-1',
  enrolled_at: '2026-06-10T12:00:00Z',
  last_seen: '2026-06-10T12:01:00Z',
  status: 'online',
  identifying_attributes: { 'service.instance.id': 'uid-42' },
  non_identifying_attributes: { 'host.arch': 'arm64' },
  hostname: 'web-prod-01',
  os: 'linux',
  version: '1.2.3',
} as unknown as CollectorInstanceView;

const sources = [
  { id: 's1', name: 'Syslog', type: 'file', enabled: true },
  { id: 's2', name: 'System Journal', type: 'journald', enabled: true },
  { id: 's3', name: 'Windows Event Log', type: 'windows_event_log', enabled: true },
] as Array<Source>;

const logPreview = {
  sourceLogs: {
    messages: [{ id: 'm1', timestamp: '2026-06-10T12:00:30.000Z', text: 'a source log line' }],
    total: 23,
  },
  selfLogs: {
    messages: [{ id: 'm2', timestamp: '2026-06-10T12:00:10.000Z', text: 'collector started' }],
    total: 7,
  },
  selfLogsError: null,
  sourceLogsError: null,
  isLoading: false,
};

describe('ConnectionSuccess', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useCollectorLogPreview).mockReturnValue(logPreview);
    asMock(useSources).mockReturnValue({ data: sources } as ReturnType<typeof useSources>);
  });

  it('shows real instance data', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByText('web-prod-01')).toBeInTheDocument();
    expect(screen.getByText('1.2.3')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Default Fleet' })).toHaveLength(2);
  });

  it('previews source logs for the connected instance', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(useCollectorLogPreview).toHaveBeenCalledWith('uid-42');
    expect(screen.getByText(/a source log line/)).toBeInTheDocument();
  });

  it('walks through the completed onboarding steps', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(useSources).toHaveBeenCalledWith('fleet-1');
    expect(screen.getByText('Connected')).toBeInTheDocument();
    expect(screen.getByText(/3 sources from fleet/)).toBeInTheDocument();
    expect(screen.getByText(/23 messages in the last 15 minutes/)).toBeInTheDocument();
  });

  it('spins on the first-messages step until source messages arrive', () => {
    asMock(useCollectorLogPreview).mockReturnValue({
      ...logPreview,
      sourceLogs: { messages: [], total: 0 },
    });

    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByText('Listening... usually under a minute')).toBeInTheDocument();
    expect(screen.getAllByText('Waiting for first messages...').length).toBeGreaterThan(0);
    expect(screen.getByText('Checking every few seconds')).toBeInTheDocument();
  });

  it('marks sources that cannot apply to the collector platform', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByText('Not applicable on Linux')).toBeInTheDocument();
  });

  it('reveals all attributes behind the toggle', async () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.queryByText('arm64')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Show all 2 attributes' }));

    expect(screen.getByText('arm64')).toBeInTheDocument();
  });

  it('falls back to the instance uid when hostname is missing', () => {
    render(<ConnectionSuccess instance={{ ...instance, hostname: null }} fleetName="Default Fleet" />);

    expect(screen.getByText('uid-42')).toBeInTheDocument();
  });

  it('renders the what-is-next links', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByRole('link', { name: 'Explore your data' })).toBeInTheDocument();
    // Also offered as the Log sources section action, hence two of them.
    expect(screen.getAllByRole('link', { name: 'Configure sources' })).toHaveLength(2);
    expect(screen.getByRole('link', { name: 'Manage fleets' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Install another collector' })).toBeInTheDocument();
  });

  it('switches to the troubleshooting view when the collector is offline', () => {
    render(<ConnectionSuccess instance={{ ...instance, status: 'offline' }} fleetName="Default Fleet" />);

    expect(screen.getByText('Connection lost')).toBeInTheDocument();
    expect(screen.getByText('Get it back online')).toBeInTheDocument();
    expect(screen.getAllByText('Paused — collector offline').length).toBeGreaterThan(0);
    expect(screen.getByRole('link', { name: 'View instances' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Check again' })).toBeInTheDocument();
    // The preview switches from source messages to the collector's own logs.
    expect(screen.getByText(/collector started/)).toBeInTheDocument();
    expect(screen.queryByText(/a source log line/)).not.toBeInTheDocument();
  });
});
