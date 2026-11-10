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
import { stream } from 'fixtures/streams';
import useStream from 'components/streams/hooks/useStream';
import { testMatch } from 'api/streams';

import StreamRulesEditor from './StreamRulesEditor';

jest.mock('components/streams/hooks/useStream');
jest.mock('api/streams', () => ({ testMatch: jest.fn() }));
jest.mock('logic/telemetry/useSendTelemetry', () => () => jest.fn());

const mockMessageWithBigInt = {
  id: 'message-id',
  index: 'graylog_0',
  fields: {
    message: 'test message',
    big_number: BigInt('9223372036854775807'),
  },
};

jest.mock(
  'components/messageloaders/LoaderTabs',
  () =>
    ({ onMessageLoaded }: { onMessageLoaded: (message: typeof mockMessageWithBigInt) => void }) => (
      <button type="button" onClick={() => onMessageLoaded(mockMessageWithBigInt)}>
        Load message
      </button>
    ),
);

describe('StreamRulesEditor', () => {
  beforeEach(() => {
    asMock(useStream).mockReturnValue({ data: stream, refetch: jest.fn(), isFetching: false, isError: false });
    asMock(testMatch).mockResolvedValue({ matches: true, rules: {} });
  });

  it('tests a loaded message containing BigInt values against the stream rules', async () => {
    render(<StreamRulesEditor streamId={stream.id} />);

    await userEvent.click(await screen.findByRole('button', { name: /load message/i }));

    await screen.findByText(/this message would be routed to this stream/i);

    expect(testMatch).toHaveBeenCalledWith(stream.id, { message: mockMessageWithBigInt.fields });
  });
});
