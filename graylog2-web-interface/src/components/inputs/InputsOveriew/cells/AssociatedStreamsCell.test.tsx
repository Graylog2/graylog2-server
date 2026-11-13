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
import useEntityTitles from 'hooks/useEntityTitles';
import { useInputMetricsFor } from 'components/inputs/InputsOveriew/InputMetricsContext';

import AssociatedStreamsCell from './AssociatedStreamsCell';

jest.mock('components/inputs/InputsOveriew/InputMetricsContext', () => ({
  useInputMetricsFor: jest.fn(),
}));
jest.mock('hooks/useEntityTitles');

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

const mockMessagesPerStream = (messagesPerStream: Record<string, number>) =>
  asMock(useInputMetricsFor).mockReturnValue({
    metrics: { messages_per_stream: messagesPerStream },
    isInitialLoading: false,
    isError: false,
  });

describe('AssociatedStreamsCell', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useEntityTitles).mockReturnValue({
      titlesById: { 'stream-a': 'Alpha Stream', 'stream-b': 'Beta Stream' },
      notPermittedIds: new Set(),
      isInitialLoading: false,
      isFetching: false,
      isError: false,
    });
  });

  it('renders the first stream and the number of the others', async () => {
    mockMessagesPerStream({ 'stream-b': 5, 'stream-a': 10 });

    render(<AssociatedStreamsCell input={input} />);

    await screen.findByRole('link', { name: 'Alpha Stream' });

    expect(await screen.findByRole('button', { name: /show 1 more/i })).toHaveTextContent('+1');
    expect(screen.queryByText('Beta Stream')).not.toBeInTheDocument();
  });

  it('links each stream to its page when the list is expanded', async () => {
    mockMessagesPerStream({ 'stream-a': 10, 'stream-b': 5 });

    render(<AssociatedStreamsCell input={input} />);

    await userEvent.click(await screen.findByRole('button', { name: /show 1 more/i }));

    expect(await screen.findByRole('link', { name: 'Alpha Stream' })).toHaveAttribute('href', '/streams/stream-a/view');
    expect(await screen.findByRole('link', { name: 'Beta Stream' })).toHaveAttribute('href', '/streams/stream-b/view');
  });

  it('renders a dash when no streams have received messages', async () => {
    mockMessagesPerStream({});

    render(<AssociatedStreamsCell input={input} />);

    await screen.findByText('—');

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('renders a spinner while metrics are loading and no cached data is present', async () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: undefined,
      isInitialLoading: true,
      isError: false,
    });

    render(<AssociatedStreamsCell input={input} />);

    await screen.findByText(/loading/i);
  });

  it('renders a dash when the request errored', async () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: undefined,
      isInitialLoading: false,
      isError: true,
    });

    render(<AssociatedStreamsCell input={input} />);

    await screen.findByText('—');
  });
});
