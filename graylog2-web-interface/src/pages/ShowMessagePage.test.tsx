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
import { render, waitFor, screen } from 'wrappedTestingLibrary';
import { StoreMock as MockStore, asMock } from 'helpers/mocking';

import DefaultQueryClientProvider from 'contexts/DefaultQueryClientProvider';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';

import ShowMessagePage from './ShowMessagePage';
import { message, event, input } from './ShowMessagePage.fixtures';

jest.mock('views/components/messagelist/MessageDetail',
  () => (props) => <span>{JSON.stringify(props, null, 2)}</span>);

const mockLoadMessage = jest.fn();
const mockGetInput = jest.fn();
const mockListNodes = jest.fn();
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const mockListStreams = jest.fn((...args) => Promise.resolve([]));

jest.mock('injection/CombinedProvider', () => ({
  get: jest.fn((type) => ({
    Preferences: {
      PreferencesStore: MockStore(),
    },
  }[type])),
}));

jest.mock('stores/nodes/NodesStore', () => ({
  NodesActions: { list: (...args) => mockListNodes(...args) },
  NodesStore: MockStore(['listen', () => () => {}], ['getInitialState', () => ({ nodes: {} })]),
}));

jest.mock('stores/messages/MessagesStore', () => ({
  MessagesActions: { loadMessage: (...args) => mockLoadMessage(...args) },
}));

jest.mock('stores/inputs/InputsStore', () => ({
  InputsActions: {
    get: jest.fn((...args) => mockGetInput(...args)),
  },
  InputsStore: MockStore(),
}));

jest.mock('stores/streams/StreamsStore', () => ({ listStreams: (...args) => mockListStreams(...args) }));

jest.mock('views/logic/fieldtypes/useFieldTypes');
jest.mock('routing/withParams', () => (x) => x);

type SimpleShowMessagePageProps = {
  index: string,
  messageId: string,
};

const SimpleShowMessagePage = ({ index, messageId }: SimpleShowMessagePageProps) => (
  <DefaultQueryClientProvider>
    {/* @ts-expect-error */}
    <ShowMessagePage params={{ index, messageId }} />
  </DefaultQueryClientProvider>
);

describe('ShowMessagePage', () => {
  beforeEach(() => {
    asMock(useFieldTypes).mockReturnValue({ data: [] });
  });

  it('triggers a node list refresh on mount', async () => {
    mockLoadMessage.mockImplementation(() => Promise.resolve(message));
    mockGetInput.mockImplementation(() => Promise.resolve(input));

    render(<SimpleShowMessagePage index="graylog_5" messageId="20f683d2-a874-11e9-8a11-0242ac130004" />);

    await waitFor(() => expect(mockListNodes).toHaveBeenCalled());
  });

  it('renders for generic message', async () => {
    mockLoadMessage.mockImplementation(() => Promise.resolve(message));
    mockGetInput.mockImplementation(() => Promise.resolve(input));

    const { container } = render(<SimpleShowMessagePage index="graylog_5" messageId="20f683d2-a874-11e9-8a11-0242ac130004" />);

    await screen.findByText(/Deprecated field/);

    expect(container).toMatchSnapshot();
  });

  it('renders for generic event', async () => {
    mockLoadMessage.mockImplementation(() => Promise.resolve(event));
    mockGetInput.mockImplementation(() => Promise.resolve());

    const { container } = render(<SimpleShowMessagePage index="gl-events_0" messageId="01DFZQ64CMGV30NT7DW2P7HQX2" />);

    await screen.findByText(/SSH Brute Force/);

    expect(container).toMatchSnapshot();
  });
});
