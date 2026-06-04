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

import MessageCountCell from './MessageCountCell';

jest.mock('components/streams/StreamsOverview/StreamMetricsContext', () => ({
  useStreamMetricsFor: jest.fn(),
}));

const stream = { id: 'stream-1', title: 'Test Stream' } as any;

describe('MessageCountCell (Streams)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the 24h message count', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { message_count: 126648 },
      isInitialLoading: false,
      isError: false,
    });

    render(<MessageCountCell stream={stream} />);

    expect(screen.getByText('126,648')).toBeInTheDocument();
  });

  it('renders an empty cell when the count is zero', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { message_count: 0 },
      isInitialLoading: false,
      isError: false,
    });

    render(<MessageCountCell stream={stream} />);

    expect(screen.queryByText('0')).not.toBeInTheDocument();
    expect(screen.queryByTitle(/messages in the last 24 hours/i)).not.toBeInTheDocument();
  });

  it('renders a spinner while loading with no cached data', async () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: undefined,
      isInitialLoading: true,
      isError: false,
    });

    render(<MessageCountCell stream={stream} />);

    expect(await screen.findByText(/loading/i)).toBeInTheDocument();
  });

  it('renders an empty cell on error', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: undefined,
      isInitialLoading: false,
      isError: true,
    });

    render(<MessageCountCell stream={stream} />);

    expect(screen.queryByTitle(/messages in the last 24 hours/i)).not.toBeInTheDocument();
  });
});
