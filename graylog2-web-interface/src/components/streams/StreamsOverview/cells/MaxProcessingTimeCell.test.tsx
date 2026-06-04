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

import MaxProcessingTimeCell from './MaxProcessingTimeCell';

jest.mock('components/streams/StreamsOverview/StreamMetricsContext', () => ({
  useStreamMetricsFor: jest.fn(),
}));

const stream = { id: 'stream-1', title: 'Test Stream' } as any;

describe('MaxProcessingTimeCell (Streams)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the formatted max processing time below the warning threshold', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { max_processing_time_ms: 155589.0 },
      isInitialLoading: false,
      isError: false,
    });

    render(<MaxProcessingTimeCell stream={stream} />);

    expect(screen.getByText(/2 min 36 s/)).toBeInTheDocument();
  });

  it('renders an empty cell when the value is zero', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { max_processing_time_ms: 0 },
      isInitialLoading: false,
      isError: false,
    });

    render(<MaxProcessingTimeCell stream={stream} />);

    expect(screen.queryByTitle(/max processing time/i)).not.toBeInTheDocument();
  });

  it('renders an empty cell when the value is missing', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: {},
      isInitialLoading: false,
      isError: false,
    });

    render(<MaxProcessingTimeCell stream={stream} />);

    expect(screen.queryByTitle(/max processing time/i)).not.toBeInTheDocument();
  });
});
