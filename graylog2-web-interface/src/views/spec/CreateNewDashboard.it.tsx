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
import { asMock, StoreMock as MockStore } from 'helpers/mocking';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';
import * as Immutable from 'immutable';

import history from 'util/History';
import Routes from 'routing/Routes';
import AppRouter from 'routing/AppRouter';
import CurrentUserProvider from 'contexts/CurrentUserProvider';
import viewsBindings from 'views/bindings';
import StreamsContext from 'contexts/StreamsContext';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import Parameter from 'views/logic/parameters/Parameter';
import { SearchMetadataActions, SearchMetadataStore } from 'views/stores/SearchMetadataStore';

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

jest.mock('views/stores/SearchMetadataStore', () => ({
  SearchMetadataActions: {
    parseSearch: jest.fn(),
  },
  SearchMetadataStore: MockStore(),
}));

declare global {
  namespace NodeJS {
    interface Global {
      api_url: string
    }
  }
}

jest.mock('util/AppConfig', () => ({
  gl2ServerUrl: jest.fn(() => global.api_url),
  gl2AppPathPrefix: jest.fn(() => ''),
  gl2DevMode: jest.fn(() => false),
  isFeatureEnabled: jest.fn(() => true),
  isCloud: jest.fn(() => false),
}));

jest.mock('stores/sessions/SessionStore', () => ({
  isLoggedIn: jest.fn(() => true),
  getSessionId: jest.fn(() => 'foobar'),
}));

jest.mock('views/components/searchbar/QueryInput', () => () => <span>Query Editor</span>);

jest.unmock('logic/rest/FetchProvider');

const viewsPlugin = new PluginManifest({}, viewsBindings);

const finderTimeout = applyTimeoutMultiplier(15000);
const testTimeout = applyTimeoutMultiplier(30000);

describe('Create a new dashboard', () => {
  beforeAll(() => PluginStore.register(viewsPlugin));

  beforeEach(() => {
    const searchMetadata = SearchMetadata.empty();
    asMock(SearchMetadataStore.getInitialState).mockReturnValue(searchMetadata);
    asMock(SearchMetadataActions.parseSearch).mockReturnValue(Promise.resolve(searchMetadata));
  });

  afterAll(() => PluginStore.unregister(viewsPlugin));

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

    const buttons = await findAllByText('Create new dashboard', {}, { timeout: finderTimeout });

    fireEvent.click(buttons[0]);
    await findByText(/This dashboard has no widgets yet/, {}, { timeout: finderTimeout });
  }, testTimeout);

  it('by going to the new dashboards endpoint', async () => {
    const { findByText } = render(<SimpleAppRouter />);

    history.push(Routes.pluginRoute('DASHBOARDS_NEW'));

    await findByText(/This dashboard has no widgets yet/, {}, { timeout: finderTimeout });
  }, testTimeout);
});
