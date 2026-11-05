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

import { asMock } from 'helpers/mocking';
import useEntityTitles from 'hooks/useEntityTitles';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';

import RoutingPipelinesCell from './RoutingPipelinesCell';

jest.mock('components/streams/StreamsOverview/StreamMetricsContext', () => ({
  useStreamMetricsFor: jest.fn(),
}));
jest.mock('hooks/useEntityTitles');

const stream = { id: 'stream-1', title: 'Test Stream' } as any;

const mockRoutingPipelines = (pipelineIds: Array<string>) =>
  asMock(useStreamMetricsFor).mockReturnValue({
    metrics: { routing_pipelines: pipelineIds },
    isInitialLoading: false,
    isError: false,
  });

describe('RoutingPipelinesCell', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useEntityTitles).mockReturnValue({
      titlesById: { 'p-1': 'Alpha Routing', 'p-2': 'Beta Routing', 'p-3': 'Gamma Routing' },
      notPermittedIds: new Set(),
      isInitialLoading: false,
      isFetching: false,
      isError: false,
    });
  });

  it('renders the first routing pipeline and the number of the others', async () => {
    mockRoutingPipelines(['p-3', 'p-1', 'p-2']);

    render(<RoutingPipelinesCell stream={stream} />);

    await screen.findByRole('link', { name: 'Alpha Routing' });

    expect(await screen.findByRole('button', { name: /show 2 more/i })).toHaveTextContent('+2');
  });

  it('renders an empty cell when there are no routing pipelines', () => {
    mockRoutingPipelines([]);

    render(<RoutingPipelinesCell stream={stream} />);

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
