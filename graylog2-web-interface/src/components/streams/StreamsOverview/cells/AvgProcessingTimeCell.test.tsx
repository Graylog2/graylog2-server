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
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';

import AvgProcessingTimeCell from './AvgProcessingTimeCell';

jest.mock('components/streams/StreamsOverview/StreamMetricsContext', () => ({
  useStreamMetricsFor: jest.fn(),
}));

const stream = { id: 'stream-1', title: 'Test Stream' } as any;

describe('AvgProcessingTimeCell (Streams)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the formatted avg processing time', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { avg_processing_time_ms: 3094.12 },
      isInitialLoading: false,
      isError: false,
    });

    render(<AvgProcessingTimeCell stream={stream} />);

    expect(screen.getByText('3.1 s')).toBeInTheDocument();
  });

  it('renders an empty cell when the value is missing', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: {},
      isInitialLoading: false,
      isError: false,
    });

    render(<AvgProcessingTimeCell stream={stream} />);

    expect(screen.queryByTitle(/average processing time/i)).not.toBeInTheDocument();
  });

  it('renders an empty cell when the value is zero', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { avg_processing_time_ms: 0 },
      isInitialLoading: false,
      isError: false,
    });

    render(<AvgProcessingTimeCell stream={stream} />);

    expect(screen.queryByTitle(/average processing time/i)).not.toBeInTheDocument();
  });

  it('renders an empty cell on error', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: undefined,
      isInitialLoading: false,
      isError: true,
    });

    render(<AvgProcessingTimeCell stream={stream} />);

    expect(screen.queryByTitle(/average processing time/i)).not.toBeInTheDocument();
  });

  it('renders a spinner while loading with no cached data', async () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: undefined,
      isInitialLoading: true,
      isError: false,
    });

    render(<AvgProcessingTimeCell stream={stream} />);

    expect(await screen.findByText(/loading/i)).toBeInTheDocument();
  });

  it('exposes the precise ms value via the title attribute', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { avg_processing_time_ms: 3094.12 },
      isInitialLoading: false,
      isError: false,
    });

    render(<AvgProcessingTimeCell stream={stream} />);

    expect(screen.getByTitle(/3094\.12 ms/)).toBeInTheDocument();
  });
});
