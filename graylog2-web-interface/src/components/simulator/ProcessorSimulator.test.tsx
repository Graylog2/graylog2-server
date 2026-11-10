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
import userEvent from '@testing-library/user-event';

import { PipelinesSimulator } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import type { Stream } from 'logic/streams/types';
import type { Message } from 'views/components/messagelist/Types';

import ProcessorSimulator from './ProcessorSimulator';

jest.mock('@graylog/server-api', () => ({
  PipelinesSimulator: {
    simulate: jest.fn(),
  },
}));

jest.mock(
  './SimulationResults',
  () =>
    ({
      originalMessage = undefined,
      simulationResults = undefined,
      isLoading = false,
    }: {
      originalMessage?: { id: string };
      simulationResults?: { took_microseconds: number };
      isLoading?: boolean;
    }) => (
      <div data-testid="simulation-results">
        {isLoading ? <span>Simulating...</span> : null}
        {originalMessage ? <span>Loaded message: {originalMessage.id}</span> : null}
        {simulationResults ? <span>Simulation took {simulationResults.took_microseconds} µs</span> : null}
      </div>
    ),
);

jest.mock('logic/message/MessageFormatter', () => ({
  formatMessageSummary: (msg: unknown) => msg,
}));

const testMessage: Message = {
  id: 'test-message-id',
  index: 'graylog_0',
  fields: { message: 'hello world' },
};

let mockLoadedMessage: Message | undefined = testMessage;
let mockLoadedOptions: { inputId?: string } = { inputId: 'input-1' };

jest.mock(
  'components/messageloaders/RawMessageLoader',
  () =>
    ({ onMessageLoaded }: { onMessageLoaded: (msg: Message | undefined, opts: { inputId?: string }) => void }) => (
      <button type="button" onClick={() => onMessageLoaded(mockLoadedMessage, mockLoadedOptions)}>
        Load message
      </button>
    ),
);

const defaultStream = {
  id: '000000000000000000000001',
  title: 'Default Stream',
} as Stream;

const customStream = {
  id: 'stream-2',
  title: 'Other Stream',
} as Stream;

describe('<ProcessorSimulator>', () => {
  beforeEach(() => {
    mockLoadedMessage = testMessage;
    mockLoadedOptions = { inputId: 'input-1' };
    asMock(PipelinesSimulator.simulate).mockResolvedValue({
      messages: [],
      took_microseconds: 4242,
    } as never);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('shows a "no streams found" panel when streams list is empty', () => {
    render(<ProcessorSimulator streams={[]} />);

    expect(screen.getByText(/no streams found/i)).toBeInTheDocument();
  });

  it('renders simulation results on the very first message load (regression test)', async () => {
    render(<ProcessorSimulator streams={[defaultStream, customStream]} />);

    await userEvent.click(screen.getByRole('button', { name: /load message/i }));

    expect(await screen.findByText(/simulation took 4242 µs/i)).toBeInTheDocument();
    expect(screen.getByText(/loaded message: test-message-id/i)).toBeInTheDocument();
  });

  it('does not render simulation results when the loaded message is undefined', async () => {
    mockLoadedMessage = undefined;
    mockLoadedOptions = {};

    render(<ProcessorSimulator streams={[defaultStream]} />);

    await userEvent.click(screen.getByRole('button', { name: /load message/i }));

    await waitFor(() => {
      expect(screen.queryByText(/simulation took/i)).not.toBeInTheDocument();
    });
    expect(screen.queryByText(/loaded message:/i)).not.toBeInTheDocument();
  });
});
