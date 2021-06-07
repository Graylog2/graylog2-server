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
import userEvent from '@testing-library/user-event';

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
  it('shows server input select when no forwarder plugin is installed', () => {
    asMock(PluginStore.exports).mockReturnValue([]);

    render(<RecentMessageLoader onMessageLoaded={jest.fn()} inputs={inputs} />);

    expect(screen.getByText(/select an input from the list below/i)).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /server input select/i })).toBeInTheDocument();
    expect(screen.getByDisplayValue(/select an input/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /load message/i })).toBeInTheDocument();
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

      render(<RecentMessageLoader onMessageLoaded={jest.fn()} inputs={inputs} />);

      expect(screen.getByRole('combobox', { name: /input type select/i })).toBeInTheDocument();

      const inputTypeDropdown = screen.getByDisplayValue(/select an input type/i);

      expect(inputTypeDropdown).toBeInTheDocument();

      userEvent.selectOptions(inputTypeDropdown, ['server']);

      expect(screen.getByText(/select an input from the list below/i)).toBeInTheDocument();
      expect(screen.getByRole('combobox', { name: /server input select/i })).toBeInTheDocument();
      expect(screen.getByDisplayValue(/select an input/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /load message/i })).toBeInTheDocument();
      expect(screen.queryByText(/select an input profile from the list/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/forwarder inputs/i)).not.toBeInTheDocument();

      userEvent.selectOptions(inputTypeDropdown, ['forwarder']);

      expect(screen.getByText(/select an input profile from the list/i)).toBeInTheDocument();
      expect(screen.getByText(/forwarder inputs/i)).toBeInTheDocument();
      expect(screen.queryByRole('combobox', { name: /server input select/i })).not.toBeInTheDocument();
    });

    it('shows only forwarder input select on cloud', () => {
      asMock(AppConfig.isCloud).mockImplementation(() => true);

      render(<RecentMessageLoader onMessageLoaded={jest.fn()} inputs={inputs} />);

      expect(screen.getByText(/select an input profile from the list/i)).toBeInTheDocument();
      expect(screen.getByText(/forwarder inputs/i)).toBeInTheDocument();

      expect(screen.queryByRole('combobox', { name: /server input select/i })).not.toBeInTheDocument();
      expect(screen.queryByDisplayValue(/select an input/i)).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /load message/i })).not.toBeInTheDocument();
    });
  });
});
