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
import { useInputMetricsFor } from 'components/inputs/InputsOveriew/InputMetricsContext';
import useEntityTitles from 'hooks/useEntityTitles';

import MessagesPerStreamSection from './MessagesPerStreamSection';

jest.mock('components/inputs/InputsOveriew/InputMetricsContext', () => ({
  useInputMetricsFor: jest.fn(),
}));
jest.mock('hooks/useEntityTitles', () => jest.fn());

const input = {
  id: 'input-1',
  title: 'My Test Input',
  type: 'org.graylog2.inputs.raw.tcp.RawTCPInput',
  name: 'Raw/Plaintext TCP',
  global: true,
  node: '',
  created_at: '2024-01-01T00:00:00Z',
  creator_user_id: 'admin',
  attributes: {},
  static_fields: {},
  content_pack: '',
};

describe('MessagesPerStreamSection', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useEntityTitles).mockReturnValue({
      titlesById: { 'stream-a': 'Stream Alpha', 'stream-b': 'Stream Beta' },
      notPermittedIds: new Set(),
      isFetching: false,
      isInitialLoading: false,
      isError: false,
    });
  });

  it('renders a row per stream with resolved titles and counts', () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: { messages_per_stream: { 'stream-a': 100, 'stream-b': 250 } },
      isInitialLoading: false,
      isError: false,
    });

    render(<MessagesPerStreamSection input={input} />);

    expect(screen.getByRole('link', { name: 'Stream Alpha' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Stream Beta' })).toBeInTheDocument();
    expect(screen.getByText('100')).toBeInTheDocument();
    expect(screen.getByText('250')).toBeInTheDocument();
  });

  it('marks deleted streams as "<id> (deleted)" when no title is returned', () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: { messages_per_stream: { 'stream-a': 100, 'stream-deleted': 5 } },
      isInitialLoading: false,
      isError: false,
    });

    render(<MessagesPerStreamSection input={input} />);

    expect(screen.getByText('stream-deleted (deleted)')).toBeInTheDocument();
  });

  it('shows an empty-state message when the stream map is empty', () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: { messages_per_stream: {} },
      isInitialLoading: false,
      isError: false,
    });

    render(<MessagesPerStreamSection input={input} />);

    expect(screen.getByText(/no streams have received messages/i)).toBeInTheDocument();
  });

  it('shows an error message when metrics loading errored', () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: undefined,
      isInitialLoading: false,
      isError: true,
    });

    render(<MessagesPerStreamSection input={input} />);

    expect(screen.getByText(/could not load messages per stream/i)).toBeInTheDocument();
  });
});
