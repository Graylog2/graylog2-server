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
import selectEvent from 'helpers/selectEvent';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useGraphDays from 'components/common/Graph/contexts/useGraphDays';
import useGraphWidth from 'components/common/Graph/useGraphWidth';
import useClusterTraffic from 'components/cluster/hooks/useClusterTraffic';

import ClusterTrafficGraph from './ClusterTrafficGraph';

jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('components/common/Graph/contexts/useGraphDays');
jest.mock('components/common/Graph/useGraphWidth');
jest.mock('components/cluster/hooks/useClusterTraffic');

const GIB = 1024 * 1024 * 1024;

describe('ClusterTrafficGraph', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendTelemetry).mockReturnValue(jest.fn());
    asMock(useGraphDays).mockReturnValue({
      graphDays: 30,
      setGraphDays: jest.fn(),
    });
    asMock(useGraphWidth).mockReturnValue({
      graphWidth: 1000,
      graphContainerRef: React.createRef(),
    });
    asMock(useClusterTraffic).mockReturnValue({
      isLoading: false,
      traffic: {
        from: '2022-09-01T00:00:00.000Z',
        to: '2022-09-03T00:00:00.000Z',
        input: {
          '2022-09-01T00:00:00.000Z': 2 * GIB,
          '2022-09-02T00:00:00.000Z': 2 * GIB,
        },
        output: {
          '2022-09-01T00:00:00.000Z': 2 * GIB,
          '2022-09-02T00:00:00.000Z': 4 * GIB,
        },
        decoded: {
          '2022-09-01T00:00:00.000Z': 2 * GIB,
          '2022-09-02T00:00:00.000Z': 2 * GIB,
        },
        input_indexed: {
          '2022-09-01T00:00:00.000Z': GIB,
          '2022-09-02T00:00:00.000Z': 2 * GIB,
        },
      },
    });
  });

  it('shows incoming (input_indexed) traffic by default', async () => {
    render(<ClusterTrafficGraph />);

    await screen.findByRole('heading', { name: /Incoming traffic/ });
    expect(screen.getByText(/Last 30 days: 3(\.0)?\s?GiB/)).toBeInTheDocument();
  });

  it('switches to output traffic when selecting the outgoing view', async () => {
    render(<ClusterTrafficGraph />);

    await selectEvent.select(await screen.findByLabelText('Show traffic type'), 'Outgoing traffic');

    await screen.findByRole('heading', { name: /Outgoing traffic/ });
    expect(screen.getByText(/Last 30 days: 6(\.0)?\s?GiB/)).toBeInTheDocument();
  });

  it('handles missing input_indexed data and still allows switching to outgoing traffic', async () => {
    asMock(useClusterTraffic).mockReturnValue({
      isLoading: false,
      traffic: {
        from: '2022-09-01T00:00:00.000Z',
        to: '2022-09-03T00:00:00.000Z',
        input: {
          '2022-09-01T00:00:00.000Z': 2 * GIB,
        },
        output: {
          '2022-09-01T00:00:00.000Z': 2 * GIB,
          '2022-09-02T00:00:00.000Z': 4 * GIB,
        },
        decoded: {
          '2022-09-01T00:00:00.000Z': 2 * GIB,
        },
      },
    });

    render(<ClusterTrafficGraph />);

    await screen.findByRole('heading', { name: /Incoming traffic/ });
    expect(screen.queryByText(/Last 30 days:/)).not.toBeInTheDocument();

    await selectEvent.select(await screen.findByLabelText('Show traffic type'), 'Outgoing traffic');

    await screen.findByRole('heading', { name: /Outgoing traffic/ });
    expect(screen.getByText(/Last 30 days: 6(\.0)?\s?GiB/)).toBeInTheDocument();
  });
});
