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

import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import useCreateStreamRule from './useCreateStreamRule';

const mockCreateStreamRule = jest.fn();
const mockResumeStream = jest.fn();
const mockSendTelemetry = jest.fn();
const mockInvalidateQueries = jest.fn();
const mockUserNotificationSuccess = jest.fn();

jest.mock('stores/streams/StreamRulesStore', () => ({
  StreamRulesStore: {
    create: (...args) => mockCreateStreamRule(...args),
  },
}));

jest.mock('stores/streams/StreamsStore', () => ({
  __esModule: true,
  default: {
    resume: (...args) => mockResumeStream(...args),
  },
}));

jest.mock('logic/telemetry/useSendTelemetry', () => () => (...args) => mockSendTelemetry(...args));

jest.mock('@tanstack/react-query', () => ({
  ...jest.requireActual('@tanstack/react-query'),
  useQueryClient: () => ({
    invalidateQueries: (...args) => mockInvalidateQueries(...args),
  }),
}));

jest.mock('util/UserNotification', () => ({
  __esModule: true,
  default: {
    success: (...args) => mockUserNotificationSuccess(...args),
  },
}));

const streamRule = {
  field: 'source',
  type: 1,
  value: 'example',
  inverted: false,
  description: 'desc',
};

type TestComponentProps = {
  streamIsPaused: boolean;
};

const TestComponent = ({ streamIsPaused }: TestComponentProps) => {
  const {
    onCreateStreamRule,
    showStartStreamDialog,
    onCancelStartStreamDialog,
    onStartStream,
  } = useCreateStreamRule({
    streamId: 'stream-id',
    streamIsPaused,
  });

  return (
    <div>
      <button type="button" onClick={() => onCreateStreamRule(undefined, streamRule)}>
        create-rule
      </button>
      {showStartStreamDialog && (
        <>
          <span>start-dialog-open</span>
          <button type="button" onClick={onStartStream}>
            start-stream
          </button>
          <button type="button" onClick={onCancelStartStreamDialog}>
            cancel-start
          </button>
        </>
      )}
    </div>
  );
};

describe('useCreateStreamRule', () => {
  beforeEach(() => {
    mockCreateStreamRule.mockImplementation((_streamId, _rule, callback) => Promise.resolve().then(() => callback()));
    mockResumeStream.mockImplementation((_streamId, callback) => Promise.resolve().then(() => callback()));
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('creates a rule and does not prompt when stream is running', async () => {
    render(<TestComponent streamIsPaused={false} />);

    userEvent.click(screen.getByRole('button', { name: /create-rule/i }));

    await waitFor(() => expect(mockCreateStreamRule).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockSendTelemetry).toHaveBeenCalledTimes(1));
    expect(mockSendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_RULE_SAVED, {
      app_pathname: 'streams',
      app_action_value: 'stream-item-rule',
    });
    expect(mockUserNotificationSuccess).toHaveBeenCalledWith('Stream rule was created successfully.', 'Success');
    expect(mockInvalidateQueries).toHaveBeenCalledWith({
      queryKey: ['stream', 'stream-id'],
    });
    expect(screen.queryByText('start-dialog-open')).not.toBeInTheDocument();
  });

  it('prompts to start stream after creating a rule for paused stream', async () => {
    render(<TestComponent streamIsPaused />);

    userEvent.click(screen.getByRole('button', { name: /create-rule/i }));

    await screen.findByText('start-dialog-open');

    userEvent.click(screen.getByRole('button', { name: /start-stream/i }));

    await waitFor(() => expect(mockResumeStream).toHaveBeenCalledWith('stream-id', expect.any(Function)));
    await waitFor(() => expect(screen.queryByText('start-dialog-open')).not.toBeInTheDocument());
  });
});
