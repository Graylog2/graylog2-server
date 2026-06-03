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
import { useInputMetricsFor } from 'components/inputs/InputsOveriew/InputMetricsContext';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

import MessageCountCell from './MessageCountCell';

jest.mock('components/inputs/InputsOveriew/InputMetricsContext', () => ({
  useInputMetricsFor: jest.fn(),
}));
jest.mock('components/common/EntityDataTable/hooks/useExpandedSections', () => jest.fn());

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

describe('MessageCountCell', () => {
  const toggleSection = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useExpandedSections).mockReturnValue({ toggleSection, expandedSections: {} });
  });

  it('renders the total messages across all streams as a formatted number badge', () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: { messages_per_stream: { 'stream-a': 76249, 'stream-b': 7076 } },
      isInitialLoading: false,
      isError: false,
    });

    render(<MessageCountCell input={input} />);

    expect(screen.getByText('83325')).toBeInTheDocument();
  });

  it('renders 0 when the stream map is empty', () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: { messages_per_stream: {} },
      isInitialLoading: false,
      isError: false,
    });

    render(<MessageCountCell input={input} />);

    expect(screen.getByText('0')).toBeInTheDocument();
  });

  it('toggles the messages_per_stream section when clicked', async () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: { messages_per_stream: { 'stream-a': 10 } },
      isInitialLoading: false,
      isError: false,
    });

    render(<MessageCountCell input={input} />);
    await userEvent.click(screen.getByText('10'));

    expect(toggleSection).toHaveBeenCalledWith('input-1', 'messages_per_stream');
  });

  it('uses keyboard_arrow_up when the section is already open', () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: { messages_per_stream: { 'stream-a': 10 } },
      isInitialLoading: false,
      isError: false,
    });
    asMock(useExpandedSections).mockReturnValue({
      toggleSection,
      expandedSections: { 'input-1': ['messages_per_stream'] },
    });

    render(<MessageCountCell input={input} />);

    expect(screen.getByTitle(/hide messages per stream/i)).toBeInTheDocument();
  });

  it('renders a spinner while metrics are loading and no cached data is present', async () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: undefined,
      isInitialLoading: true,
      isError: false,
    });

    render(<MessageCountCell input={input} />);

    expect(await screen.findByText(/loading/i)).toBeInTheDocument();
  });

  it('renders a dash when the request errored', () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: undefined,
      isInitialLoading: false,
      isError: true,
    });

    render(<MessageCountCell input={input} />);

    expect(screen.getByText('—')).toBeInTheDocument();
  });

  it('renders a dash when the field is not in the metrics payload', () => {
    asMock(useInputMetricsFor).mockReturnValue({
      metrics: {},
      isInitialLoading: false,
      isError: false,
    });

    render(<MessageCountCell input={input} />);

    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
