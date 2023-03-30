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
import { PluginStore } from 'graylog-web-plugin/plugin';
import userEvent from '@testing-library/user-event';

import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import AppConfig from 'util/AppConfig';
import { inputs } from 'components/messageloaders/MessageLoaders.fixtures';

import RawMessageLoader from './RawMessageLoader';

jest.mock('stores/system/SystemStore', () => ({ SystemStore: MockStore() }));
jest.mock('stores/nodes/NodesStore', () => ({ NodesStore: MockStore() }));

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
    expect(screen.getByText(/select input/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/message codec/i)).toBeInTheDocument();
  });

  describe('with forwarder plugin installed', () => {
    beforeEach(() => {
      asMock(PluginStore.exports).mockImplementation((type) => ({
        forwarder: [{
          messageLoaders: {
            ForwarderInputDropdown: () => <>Forwarder Inputs</>,
          },
        }],
      }[type]));
    });

    it('allows user to select between server and forwarder input on premise', () => {
      asMock(AppConfig.isCloud).mockImplementation(() => false);

      render(<RawMessageLoader inputs={inputs} onMessageLoaded={jest.fn()} codecTypes={{}} inputIdSelector />);

      const inputTypeSelect = screen.getByRole('combobox', { name: /select an input type \(optional\)/i });

      expect(inputTypeSelect).toBeInTheDocument();

      userEvent.selectOptions(inputTypeSelect, ['server']);

      expect(screen.getByLabelText(/message input \(optional\)/i)).toBeInTheDocument();
      expect(screen.getByText(/select input/i)).toBeInTheDocument();
      expect(screen.queryByText(/forwarder inputs/i)).not.toBeInTheDocument();

      userEvent.selectOptions(inputTypeSelect, ['forwarder']);

      expect(screen.getByText(/forwarder inputs/i)).toBeInTheDocument();
      expect(screen.queryByLabelText(/message input \(optional\)/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/select input/i)).not.toBeInTheDocument();
    });

    it('shows only forwarder input select on cloud', () => {
      asMock(AppConfig.isCloud).mockImplementation(() => true);

      render(<RawMessageLoader inputs={inputs} onMessageLoaded={jest.fn()} codecTypes={{}} inputIdSelector />);

      expect(screen.getByText(/forwarder inputs/i)).toBeInTheDocument();
    });
  });
});
