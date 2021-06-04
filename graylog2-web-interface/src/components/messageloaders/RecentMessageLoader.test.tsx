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
import { Map } from 'immutable';
import asMock from 'helpers/mocking/AsMock';
import { PluginStore } from 'graylog-web-plugin/plugin';

import AppConfig from 'util/AppConfig';

import RecentMessageLoader from './RecentMessageLoader';
import type { Input } from './Types';

jest.mock('util/AppConfig', () => ({
  isCloud: jest.fn(() => false),
}));

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: jest.fn(),
  },
}));

const inputs = Map<string, Input>({});

describe('<RecentMessageLoader>', () => {
  it('shows server input select when no forwarder plugin is present', () => {
    asMock(PluginStore.exports).mockReturnValue([]);

    render(<RecentMessageLoader onMessageLoaded={jest.fn()} inputs={inputs} />);

    expect(screen.getByText(/select an input from the list below/i)).toBeInTheDocument();
    expect(screen.getByRole('combobox')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /load message/i })).toBeInTheDocument();
  });

  it('shows forwarder input select on cloud', () => {
    asMock(PluginStore.exports).mockImplementation((type) => ({
      forwarder: [{
        messageLoaders: {
          ForwarderInputDropdown: () => <>Forwarder Inputs</>,
        },
      }],
    }[type]));

    asMock(AppConfig.isCloud).mockImplementation(() => true);

    render(<RecentMessageLoader onMessageLoaded={jest.fn()} inputs={inputs} />);

    expect(screen.getByText(/select an input profile from the list/i)).toBeInTheDocument();
    expect(screen.getByText(/forwarder inputs/i)).toBeInTheDocument();
  });
});
