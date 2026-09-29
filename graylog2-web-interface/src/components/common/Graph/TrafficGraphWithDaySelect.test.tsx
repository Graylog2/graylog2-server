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
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import selectEvent from 'helpers/selectEvent';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useGraphDays from 'components/common/Graph/contexts/useGraphDays';
import useGraphWidth from 'components/common/Graph/useGraphWidth';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import TrafficGraphWithDaySelect from './TrafficGraphWithDaySelect';

jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('components/common/Graph/contexts/useGraphDays');
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
    asMock(useGraphWidth).mockReturnValue({
      graphWidth: 1000,
      graphContainerRef: React.createRef(),
    });
  });

  it('displays traffic graph with total and allows changing days', async () => {
    render(<TrafficGraphWithDaySelect traffic={mockTrafficData} />);

    await screen.findByText('Outgoing traffic');
    expect(screen.getByText(/Last 30 days:/)).toBeInTheDocument();

    await selectEvent.select(await screen.findByLabelText('Days'), '365');

    await waitFor(() => {
      expect(mockSetGraphDays).toHaveBeenCalledWith(365);
    });

    expect(useSendTelemetry).toHaveBeenCalledWith('outgoing-traffic');
    expect(mockSendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.TRAFFIC_GRAPH_DAYS_CHANGED, {
      app_action_value: 'trafficgraph-days-button',
      event_details: { value: 365 },
    });
  });

  it('supports both input and output traffic with correct titles and telemetry', async () => {
    const { rerender } = render(<TrafficGraphWithDaySelect traffic={mockTrafficData} trafficType="input-indexed" />);

    await screen.findByText('Incoming traffic');

    await selectEvent.select(await screen.findByLabelText('Days'), '90');

    expect(useSendTelemetry).toHaveBeenCalledWith('incoming-traffic');
    expect(mockSendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.TRAFFIC_GRAPH_DAYS_CHANGED, {
      app_action_value: 'trafficgraph-days-button',
      event_details: { value: 90 },
    });

    jest.clearAllMocks();
    rerender(<TrafficGraphWithDaySelect traffic={mockTrafficData} trafficType="output" />);

    await screen.findByText('Outgoing traffic');

    expect(mockSendTelemetry).not.toHaveBeenCalled();
  });

  it('renders a traffic type selector when onTrafficTypeChange is provided', async () => {
    const onTrafficTypeChange = jest.fn();

    render(
      <TrafficGraphWithDaySelect
        traffic={mockTrafficData}
        trafficType="input-indexed"
        onTrafficTypeChange={onTrafficTypeChange}
      />,
    );

    await selectEvent.select(await screen.findByLabelText('Show traffic type'), 'Outgoing traffic');

    await waitFor(() => {
      expect(onTrafficTypeChange).toHaveBeenCalledWith('output');
    });

    expect(useSendTelemetry).toHaveBeenCalledWith('incoming-traffic');
    expect(mockSendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.TRAFFIC_GRAPH_TYPE_CHANGED, {
      app_section: 'outgoing-traffic',
      app_action_value: 'trafficgraph-type-select',
      event_details: { value: 'output' },
    });
  });

  it('reports incoming telemetry when switching back to the incoming view', async () => {
    const onTrafficTypeChange = jest.fn();

    render(
      <TrafficGraphWithDaySelect
        traffic={mockTrafficData}
        trafficType="output"
        onTrafficTypeChange={onTrafficTypeChange}
      />,
    );

    await selectEvent.select(await screen.findByLabelText('Show traffic type'), 'Incoming traffic');

    await waitFor(() => {
      expect(onTrafficTypeChange).toHaveBeenCalledWith('input-indexed');
    });

    expect(useSendTelemetry).toHaveBeenCalledWith('outgoing-traffic');
    expect(mockSendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.TRAFFIC_GRAPH_TYPE_CHANGED, {
      app_section: 'incoming-traffic',
      app_action_value: 'trafficgraph-type-select',
      event_details: { value: 'input-indexed' },
    });
  });

  it('does not render a traffic type selector by default', async () => {
    render(<TrafficGraphWithDaySelect traffic={mockTrafficData} trafficType="input-indexed" />);

    await screen.findByText('Incoming traffic');
    expect(screen.queryByLabelText('Show traffic type')).not.toBeInTheDocument();
  });

  it('always renders a Zoom/Reset button in the controls row', async () => {
    render(<TrafficGraphWithDaySelect traffic={mockTrafficData} />);

    await screen.findByRole('button', { name: 'Zoom/Reset' });
  });

  it('disables Zoom/Reset on a pristine graph without a traffic limit', async () => {
    render(<TrafficGraphWithDaySelect traffic={mockTrafficData} />);

    expect(await screen.findByRole('button', { name: 'Zoom/Reset' })).toBeDisabled();
  });

  it('enables Zoom/Reset when a traffic limit dwarfs the data', async () => {
    render(<TrafficGraphWithDaySelect traffic={mockTrafficData} trafficLimit={1024 * 1024 * 1024} />);

    expect(await screen.findByRole('button', { name: 'Zoom/Reset' })).toBeEnabled();
  });

  it('keeps Zoom/Reset disabled when the daily total exceeds the limit even though every raw bucket is below it', async () => {
    const hourlyTraffic = {
      '2022-09-21T08:00:00.000Z': 600,
      '2022-09-21T09:00:00.000Z': 600,
    };

    render(<TrafficGraphWithDaySelect traffic={hourlyTraffic} trafficLimit={1000} />);

    expect(await screen.findByRole('button', { name: 'Zoom/Reset' })).toBeDisabled();
  });

  it('ignores re-selecting the currently selected days value', async () => {
    render(<TrafficGraphWithDaySelect traffic={mockTrafficData} />);

    await selectEvent.select(await screen.findByLabelText('Days'), '30');

    expect(mockSetGraphDays).not.toHaveBeenCalled();
    expect(mockSendTelemetry).not.toHaveBeenCalled();
  });

  it('ignores re-selecting the current traffic type', async () => {
    const onTrafficTypeChange = jest.fn();

    render(
      <TrafficGraphWithDaySelect
        traffic={mockTrafficData}
        trafficType="output"
        onTrafficTypeChange={onTrafficTypeChange}
      />,
    );

    await selectEvent.select(await screen.findByLabelText('Show traffic type'), 'Outgoing traffic');

    expect(onTrafficTypeChange).not.toHaveBeenCalled();
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

  it('shows a spinner without a total while traffic is not available yet', async () => {
    render(<TrafficGraphWithDaySelect />);

    await screen.findByText('Outgoing traffic');
    await screen.findByText(/Loading/);
    expect(screen.queryByText(/Last 30 days:/)).not.toBeInTheDocument();
  });

  it('shows a zero total when the traffic sums to zero', async () => {
    const zeroTraffic = {
      '2022-09-21T08:00:00.000Z': 0,
      '2022-09-21T09:00:00.000Z': 0,
    };

    render(<TrafficGraphWithDaySelect traffic={zeroTraffic} />);

    await screen.findByRole('heading', { name: /Outgoing traffic/ });
    expect(screen.getByText(/Last 30 days: 0(\.0)?\s?B/)).toBeInTheDocument();
  });

  it('passes trafficLimit to graph component', async () => {
    const { container } = render(<TrafficGraphWithDaySelect traffic={mockTrafficData} trafficLimit={1073741824} />);

    expect(container).toBeInTheDocument();
  });
});
