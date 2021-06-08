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

import React from 'react';
import { screen, render } from 'wrappedTestingLibrary';
import { StoreMock as MockStore, CombinedProviderMock as MockCombinedProvider } from 'helpers/mocking';

import { inputs } from 'components/messageloaders/MessageLoaders.fixtures';

import RawMessageLoader from './RawMessageLoader';

jest.mock('injection/CombinedProvider', () => new MockCombinedProvider({
  Inputs: {
    InputsActions: { list: jest.fn(() => Promise.resolve({ inputs: [] })) },
    InputsStore: MockStore(['getInitialState', () => ({ inputs: undefined })]),
  },
  CodecTypes: {
    CodecTypesActions: { list: jest.fn(() => Promise.resolve({ codecTypes: {} })) },
    CodecTypesStore: MockStore(['getInitialState', () => ({ codecTypes: undefined })]),
  },
}));

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  isCloud: jest.fn(() => false),
}));

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: jest.fn(),
  },
}));

describe('<RawMessageLoader.test>', () => {
  it('shows server input select when no forwarder plugin is installed', () => {
    render(<RawMessageLoader inputs={inputs} onMessageLoaded={jest.fn()} codecTypes={{}} inputIdSelector />);

    expect(screen.getByRole('textbox', { name: /raw message/i })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: /source ip address \(optional\)/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/message input/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/message codec/i)).toBeInTheDocument();
  });
});
