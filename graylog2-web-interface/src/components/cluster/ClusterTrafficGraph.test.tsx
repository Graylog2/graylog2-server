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
import useGraphDays from 'components/common/Graph/contexts/useGraphDays';
import useClusterTraffic from 'components/cluster/hooks/useClusterTraffic';

import ClusterTrafficGraph from './ClusterTrafficGraph';

jest.mock('components/common/Graph/contexts/useGraphDays');
jest.mock('components/cluster/hooks/useClusterTraffic');
jest.mock('components/common/Graph', () => ({
  TrafficGraphWithDaySelect: jest.fn(({ trafficType, traffic }: any) => (
    <div data-testid="traffic-graph" data-type={trafficType} data-traffic={JSON.stringify(traffic)}>
      TrafficGraph: {trafficType}
    </div>
  )),
}));

describe('ClusterTrafficGraph', () => {
  const mockTrafficData = {
    input: {
      '2022-09-21T08:00:00.000Z': 20218553,
      '2022-09-21T09:00:00.000Z': 7867447,
    },
    input_indexed: {
      '2022-09-21T08:00:00.000Z': 0,
      '2022-09-21T09:00:00.000Z': 7100000,
    },
    output: {
      '2022-09-21T08:00:00.000Z': 45410034,
      '2022-09-21T09:00:00.000Z': 17605708,
    },
    decoded: {
      '2022-09-21T08:00:00.000Z': 40711455,
      '2022-09-21T09:00:00.000Z': 25473155,
    },
    from: '2022-09-21T08:00:00.000Z',
    to: '2022-09-21T10:00:00.000Z',
  };

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useGraphDays).mockReturnValue({
      graphDays: 30,
      setGraphDays: jest.fn(),
    });
  });

  it('plots output traffic', () => {
    asMock(useClusterTraffic).mockReturnValue({
      traffic: mockTrafficData,
      isLoading: false,
    });

    render(<ClusterTrafficGraph />);

    expect(screen.getByTestId('traffic-graph')).toHaveAttribute('data-type', 'output');
    expect(screen.getByTestId('traffic-graph')).toHaveAttribute(
      'data-traffic',
      JSON.stringify({
        '2022-09-21T08:00:00.000Z': 45410034,
        '2022-09-21T09:00:00.000Z': 17605708,
      }),
    );
  });

  it('renders without traffic data', () => {
    asMock(useClusterTraffic).mockReturnValue({
      traffic: undefined,
      isLoading: false,
    });

    render(<ClusterTrafficGraph />);

    expect(screen.getByTestId('traffic-graph')).not.toHaveAttribute('data-traffic');
  });
});
