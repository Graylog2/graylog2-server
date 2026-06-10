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

import asMock from 'helpers/mocking/AsMock';
import { useSources } from 'components/collectors/hooks/useSourceQueries';
import type { CollectorInstanceView } from 'components/collectors/types';

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
  identifying_attributes: {},
  non_identifying_attributes: {},
  hostname: 'web-prod-01',
  os: 'linux',
  version: '1.2.3',
} as CollectorInstanceView;

describe('ConnectionSuccess', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useCollectorLogPreview).mockReturnValue({
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
    });

    asMock(useSources).mockReturnValue({
      data: [{ id: 's1' }, { id: 's2' }],
    } as ReturnType<typeof useSources>);
  });

  it('shows real instance data', () => {
    render(<ConnectionSuccess platformId="linux" instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByText('web-prod-01')).toBeInTheDocument();
    expect(screen.getByText(/1\.2\.3/)).toBeInTheDocument();
    expect(screen.getByText('Default Fleet')).toBeInTheDocument();
  });

  it('previews source logs for the connected instance', () => {
    render(<ConnectionSuccess platformId="linux" instance={instance} fleetName="Default Fleet" />);

    expect(useCollectorLogPreview).toHaveBeenCalledWith('uid-42');
    expect(screen.getByText(/a source log line/)).toBeInTheDocument();
  });

  it('shows the message total from the source logs preview', () => {
    render(<ConnectionSuccess platformId="linux" instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByText('23')).toBeInTheDocument();
  });

  it('shows the fleet source count', () => {
    render(<ConnectionSuccess platformId="linux" instance={instance} fleetName="Default Fleet" />);

    expect(useSources).toHaveBeenCalledWith('fleet-1');
    expect(screen.getAllByText('2').length).toBeGreaterThanOrEqual(1);
  });

  it('renders the self-logs section collapsed', () => {
    render(<ConnectionSuccess platformId="linux" instance={instance} fleetName="Default Fleet" />);

    expect(screen.getByRole('heading', { name: /collector logs/i })).toBeInTheDocument();
    expect(screen.getByTestId('collapseButton')).toBeInTheDocument();
  });

  it('falls back to the instance uid when hostname is missing', () => {
    render(
      <ConnectionSuccess platformId="linux" instance={{ ...instance, hostname: null }} fleetName="Default Fleet" />,
    );

    expect(screen.getByText('uid-42')).toBeInTheDocument();
  });

  it('omits the platform chip when platformId is not known', () => {
    render(<ConnectionSuccess instance={instance} fleetName="Default Fleet" />);

    expect(screen.queryByText('Linux')).not.toBeInTheDocument();
    expect(screen.getByText('web-prod-01')).toBeInTheDocument();
  });
});
