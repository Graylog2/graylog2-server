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
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useGraphDays from 'components/common/Graph/contexts/useGraphDays';
import useLocation from 'routing/useLocation';
import useGraphWidth from 'components/common/Graph/useGraphWidth';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import TrafficGraphWithDaySelect from './TrafficGraphWithDaySelect';

jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('components/common/Graph/contexts/useGraphDays');
jest.mock('routing/useLocation');
jest.mock('components/common/Graph/useGraphWidth');

describe('TrafficGraphWithDaySelect', () => {
  const mockSendTelemetry = jest.fn();
  const mockSetGraphDays = jest.fn();

  const mockTrafficData = {
    '2022-09-21T08:00:00.000Z': 20218553,
    '2022-09-21T09:00:00.000Z': 7867447,
    '2022-09-26T10:00:00.000Z': 7942929,
  };

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendTelemetry).mockReturnValue(mockSendTelemetry);
    asMock(useGraphDays).mockReturnValue({
      graphDays: 30,
      setGraphDays: mockSetGraphDays,
    });
    asMock(useLocation).mockReturnValue({
      pathname: '/system/overview',
      search: '',
      hash: '',
      state: undefined,
      key: 'default',
    });
    asMock(useGraphWidth).mockReturnValue({
      graphWidth: 1000,
      graphContainerRef: React.createRef(),
    });
  });

  it('displays traffic graph with total and allows changing days', async () => {
    const { getByLabelText } = render(<TrafficGraphWithDaySelect traffic={mockTrafficData} />);

    await screen.findByText('Outgoing traffic');
    expect(screen.getByText(/Last 30 days:/)).toBeInTheDocument();

    const daysSelect = getByLabelText('Days') as HTMLSelectElement;
    expect(daysSelect.value).toBe('30');

    await userEvent.selectOptions(daysSelect, '365');

    await waitFor(() => {
      expect(mockSetGraphDays).toHaveBeenCalledWith(365);
    });

    expect(mockSendTelemetry).toHaveBeenCalledWith(
      TELEMETRY_EVENT_TYPE.TRAFFIC_GRAPH_DAYS_CHANGED,
      expect.objectContaining({
        app_section: 'outgoing-traffic',
        event_details: { value: 365 },
      }),
    );
  });

  it('supports both input and output traffic with correct titles and telemetry', async () => {
    const { getByLabelText, rerender } = render(
      <TrafficGraphWithDaySelect traffic={mockTrafficData} trafficType="input-indexed" />,
    ) as any;

    await screen.findByText('Incoming traffic');

    const daysSelect = getByLabelText('Days');
    await userEvent.selectOptions(daysSelect, '90');

    expect(mockSendTelemetry).toHaveBeenCalledWith(
      TELEMETRY_EVENT_TYPE.TRAFFIC_GRAPH_DAYS_CHANGED,
      expect.objectContaining({ app_section: 'incoming-traffic' }),
    );

    jest.clearAllMocks();
    rerender(<TrafficGraphWithDaySelect traffic={mockTrafficData} trafficType="output" />);

    await screen.findByText('Outgoing traffic');

    expect(mockSendTelemetry).not.toHaveBeenCalled();
  });

  it('allows custom title to override default', async () => {
    render(
      <TrafficGraphWithDaySelect traffic={mockTrafficData} trafficType="input-indexed" title="Remaining Volume" />,
    );

    await screen.findByText('Remaining Volume');
    expect(screen.queryByText('Incoming traffic')).not.toBeInTheDocument();
  });

  it('handles empty traffic data without errors', async () => {
    render(<TrafficGraphWithDaySelect traffic={{}} />);

    await screen.findByText('Outgoing traffic');
  });

  it('passes trafficLimit to graph component', async () => {
    const { container } = render(<TrafficGraphWithDaySelect traffic={mockTrafficData} trafficLimit={1073741824} />);

    expect(container).toBeInTheDocument();
  });
});
