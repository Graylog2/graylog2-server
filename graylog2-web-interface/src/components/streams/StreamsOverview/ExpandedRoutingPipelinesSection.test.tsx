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

import ExpandedRoutingPipelinesSection from './ExpandedRoutingPipelinesSection';

jest.mock('components/streams/StreamsOverview/StreamMetricsContext', () => ({
  useStreamMetricsFor: jest.fn(),
}));
jest.mock('hooks/useEntityTitles', () => jest.fn());

const stream = { id: 'stream-1', title: 'Test Stream' } as any;

describe('ExpandedRoutingPipelinesSection', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useEntityTitles).mockReturnValue({
      titlesById: { 'pipeline-1': 'Routing Pipeline' },
      notPermittedIds: new Set(),
      isFetching: false,
      isInitialLoading: false,
      isError: false,
    });
  });

  it('renders linked routing pipeline titles', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { routing_pipelines: ['pipeline-1'] },
      isInitialLoading: false,
      isError: false,
    });

    render(<ExpandedRoutingPipelinesSection stream={stream} />);

    expect(screen.getByRole('link', { name: 'Routing Pipeline' })).toBeInTheDocument();
  });

  it('renders empty-state when there are no routing pipelines', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { routing_pipelines: [] },
      isInitialLoading: false,
      isError: false,
    });

    render(<ExpandedRoutingPipelinesSection stream={stream} />);

    expect(screen.getByText(/no routing pipelines/i)).toBeInTheDocument();
  });
});
