/**
 * @jest-environment <rootDir>/test/integration-environment.js
 */
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
import { render, fireEvent } from 'wrappedTestingLibrary';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import { StoreMock as MockStore } from 'helpers/mocking';

import history from 'util/History';
import Routes from 'routing/Routes';
import AppRouter from 'routing/AppRouter';
import CurrentUserProvider from 'contexts/CurrentUserProvider';
import viewsBindings from 'views/bindings';
import StreamsContext from 'contexts/StreamsContext';

jest.mock('views/stores/DashboardsStore', () => ({
  DashboardsActions: {
    search: jest.fn(() => Promise.resolve()),
  },
  DashboardsStore: MockStore(
    ['listen', () => jest.fn()],
    ['getInitialState', () => ({
      listen: [],
      pagination: {
        total: 100,
        page: 1,
        perPage: 20,
      },
    })],
  ),
}));

jest.mock('stores/users/CurrentUserStore', () => MockStore(
  ['listen', () => jest.fn()],
  'get',
  ['getInitialState', () => ({
    currentUser: {
      id: 'user-betty-id',
      full_name: 'Betty Holberton',
      username: 'betty',
      permissions: ['dashboards:create'],
    },
  })],
));

declare global {
  namespace NodeJS {
    interface Global {
      // eslint-disable-next-line camelcase
      api_url: string
    }
  }
}

jest.mock('util/AppConfig', () => ({
  gl2ServerUrl: jest.fn(() => global.api_url),
  gl2AppPathPrefix: jest.fn(() => ''),
  gl2DevMode: jest.fn(() => false),
  isFeatureEnabled: jest.fn(() => true),
}));

jest.mock('stores/sessions/SessionStore', () => ({
  isLoggedIn: jest.fn(() => true),
  getSessionId: jest.fn(() => 'foobar'),
}));

jest.mock('views/components/searchbar/QueryInput', () => () => <span>Query Editor</span>);

jest.unmock('logic/rest/FetchProvider');

describe('Create a new dashboard', () => {
  beforeAll(() => {
    PluginStore.register(new PluginManifest({}, viewsBindings));
  });

  beforeEach(() => {
    jest.setTimeout(30000);
  });

  const SimpleAppRouter = () => (
    <CurrentUserProvider>
      <StreamsContext.Provider value={[{ id: 'stream-1' }]}>
        <AppRouter />
      </StreamsContext.Provider>
    </CurrentUserProvider>
  );

  it('using Dashboards Page', async () => {
    const { findByText, findAllByText } = render(<SimpleAppRouter />);
    history.push(Routes.DASHBOARDS);

    const buttons = await findAllByText('Create new dashboard', {}, { timeout: 15000 });

    fireEvent.click(buttons[0]);
    await findByText(/This dashboard has no widgets yet/, {}, { timeout: 15000 });
  });

  it('by going to the new dashboards endpoint', async () => {
    const { findByText } = render(<SimpleAppRouter />);

    history.push(Routes.pluginRoute('DASHBOARDS_NEW'));

    await findByText(/This dashboard has no widgets yet/, {}, { timeout: 15000 });
  });
});
