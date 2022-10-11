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
import Immutable from 'immutable';
import { defaultUser } from 'defaultMockValues';

import { adminUser } from 'fixtures/users';
import MockAction from 'helpers/mocking/MockAction';
import MockStore from 'helpers/mocking/StoreMock';
import { asMock } from 'helpers/mocking';
import { ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import useCurrentUser from 'hooks/useCurrentUser';

import URLWhiteListFormModal from './URLWhiteListFormModal';

jest.mock('hooks/useCurrentUser');

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(['getInitialState', jest.fn(() => ({
    configuration: {
      'org.graylog2.system.urlwhitelist.UrlWhitelist': {
        entries: [],
        disabled: false,
      },
    },
  }))]),
  ConfigurationsActions: {
    listWhiteListConfig: MockAction(),
  },
}));

describe('<URLWhiteListFormModal>', () => {
  const renderSUT = () => {
    return render(
      <URLWhiteListFormModal newUrlEntry="http://graylog.com" urlType="literal" />,
    );
  };

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  it('renders elements to add URL to allow list', async () => {
    renderSUT();

    const addButton = screen.getByRole('button', { name: /add to url whitelist/i });

    expect(addButton).toBeInTheDocument();

    addButton.click();

    expect(await screen.findByText('Whitelist URLs')).toBeInTheDocument();
    expect(screen.getByDisplayValue('http://graylog.com')).toBeInTheDocument();
    expect(screen.getByText(/exact match/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /update configuration/i, hidden: true })).toBeInTheDocument();
    expect(screen.getByText(/cancel/i)).toBeInTheDocument();
  });

  it('does not render if user has no permissions to add to allow list', () => {
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().permissions(Immutable.List([])).build());
    renderSUT();

    expect(screen.queryByRole('button', { name: /add to url whitelist/i })).not.toBeInTheDocument();
  });

  it('does not render if allow list is not loaded', () => {
    asMock(ConfigurationsStore.getInitialState).mockImplementation(() => ({
      configuration: {
        'org.graylog2.system.urlwhitelist.UrlWhitelist': undefined,
      },
      searchesClusterConfig: undefined,
      eventsClusterConfig: undefined,
    }));

    renderSUT();

    expect(screen.queryByRole('button', { name: /add to url whitelist/i })).not.toBeInTheDocument();
  });

  it('extends existing allow list with given newUrlEntry', async () => {
    asMock(ConfigurationsStore.getInitialState).mockImplementation(() => ({
      configuration: {
        'org.graylog2.system.urlwhitelist.UrlWhitelist': {
          entries: [{ id: '1234', title: 'localhost', value: 'http://localhost(:\\d+)?', type: 'regex' }],
          disabled: false,
        },
      },
      searchesClusterConfig: undefined,
      eventsClusterConfig: undefined,
    }));

    renderSUT();
    const addButton = screen.queryByRole('button', { name: /add to url whitelist/i });

    expect(addButton).toBeInTheDocument();

    addButton.click();

    expect(await screen.findByText('Whitelist URLs')).toBeInTheDocument();
    expect(screen.getByDisplayValue('http://localhost(:\\d+)?')).toBeInTheDocument();
    expect(screen.getByDisplayValue('http://graylog.com')).toBeInTheDocument();
  });
});
