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

import { asMock } from 'helpers/mocking';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';

import RoutingPipelinesCell from './RoutingPipelinesCell';

jest.mock('components/streams/StreamsOverview/StreamMetricsContext', () => ({
  useStreamMetricsFor: jest.fn(),
}));
jest.mock('components/common/EntityDataTable/hooks/useExpandedSections', () => jest.fn());

const stream = { id: 'stream-1', title: 'Test Stream' } as any;

describe('RoutingPipelinesCell (Streams)', () => {
  const toggleSection = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useExpandedSections).mockReturnValue({ toggleSection, expandedSections: {} });
  });

  it('renders the count of routing pipelines', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { routing_pipelines: ['p-1', 'p-2', 'p-3'] },
      isInitialLoading: false,
      isError: false,
    });

    render(<RoutingPipelinesCell stream={stream} />);

    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('renders an empty cell when there are no routing pipelines', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { routing_pipelines: [] },
      isInitialLoading: false,
      isError: false,
    });

    render(<RoutingPipelinesCell stream={stream} />);

    expect(screen.queryByText('0')).not.toBeInTheDocument();
    expect(screen.queryByTitle(/routing pipelines/i)).not.toBeInTheDocument();
  });

  it('toggles the routing_pipelines section on click', async () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { routing_pipelines: ['p-1'] },
      isInitialLoading: false,
      isError: false,
    });

    render(<RoutingPipelinesCell stream={stream} />);
    await userEvent.click(screen.getByText('1'));

    expect(toggleSection).toHaveBeenCalledWith('stream-1', 'routing_pipelines');
  });
});
