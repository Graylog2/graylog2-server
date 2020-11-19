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
// @flow strict
import * as React from 'react';
import { render, waitFor } from 'wrappedTestingLibrary';
import { StoreMock as MockStore } from 'helpers/mocking';

import ShowMessagePage from './ShowMessagePage';
import { message, event, input } from './ShowMessagePage.fixtures';

jest.mock('views/components/messagelist/MessageDetail',
  () => (props) => <span>{JSON.stringify(props, null, 2)}</span>);

const mockLoadMessage = jest.fn();
const mockGetInput = jest.fn();
const mockListNodes = jest.fn();
const mockListStreams = jest.fn(() => Promise.resolve([]));

jest.mock('injection/CombinedProvider', () => ({
  get: jest.fn((type) => ({
    Inputs: { InputsActions: { get: (...args) => mockGetInput(...args) }, InputsStore: {} },
    Nodes: {
      NodesActions: { list: (...args) => mockListNodes(...args) },
      NodesStore: MockStore(['listen', () => () => {}], ['getInitialState', () => ({ nodes: {} })]),
    },
    Messages: { MessagesActions: { loadMessage: (...args) => mockLoadMessage(...args) } },
    Streams: { StreamsStore: { listStreams: (...args) => mockListStreams(...args) } },
    CurrentUser: {
      CurrentUserStore: MockStore(),
    },
    Preferences: {
      PreferencesStore: MockStore(),
    },
  }[type])),
}));

jest.mock('routing/withParams', () => (x) => x);

describe('ShowMessagePage', () => {
  it('triggers a node list refresh on mount', async () => {
    mockLoadMessage.mockImplementation(() => Promise.resolve(message));
    mockGetInput.mockImplementation(() => Promise.resolve(input));
    render(<ShowMessagePage params={{ index: 'graylog_5', messageId: '20f683d2-a874-11e9-8a11-0242ac130004' }} />);
    await waitFor(() => expect(mockListNodes).toHaveBeenCalled());
  });

  it('renders for generic message', async () => {
    mockLoadMessage.mockImplementation(() => Promise.resolve(message));
    mockGetInput.mockImplementation(() => Promise.resolve(input));
    const { container, queryByTestId } = render(<ShowMessagePage params={{ index: 'graylog_5', messageId: '20f683d2-a874-11e9-8a11-0242ac130004' }} />);

    await waitFor(() => expect(queryByTestId('spinner')).toBeNull());

    expect(container).toMatchSnapshot();
  });

  it('renders for generic event', async () => {
    mockLoadMessage.mockImplementation(() => Promise.resolve(event));
    mockGetInput.mockImplementation(() => Promise.resolve());
    const { container, queryByTestId } = render(<ShowMessagePage params={{ index: 'gl-events_0', messageId: '01DFZQ64CMGV30NT7DW2P7HQX2' }} />);

    await waitFor(() => expect(queryByTestId('spinner')).toBeNull());

    expect(container).toMatchSnapshot();
  });
});
