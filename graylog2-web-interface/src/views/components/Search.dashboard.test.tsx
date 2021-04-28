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
import Immutable from 'immutable';
import { render, waitFor, screen } from 'wrappedTestingLibrary';
import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import mockComponent from 'helpers/mocking/MockComponent';
import mockAction from 'helpers/mocking/MockAction';

import { StreamsActions } from 'views/stores/StreamsStore';
import { WidgetStore } from 'views/stores/WidgetStore';
import { QueryFiltersStore } from 'views/stores/QueryFiltersStore';
import { SearchActions } from 'views/stores/SearchStore';
import { SearchConfigActions } from 'views/stores/SearchConfigStore';
import { ViewStore } from 'views/stores/ViewStore';
import { FieldTypesActions } from 'views/stores/FieldTypesStore';
import { SearchMetadataStore } from 'views/stores/SearchMetadataStore';
import View from 'views/logic/views/View';

import Search from './Search';
import WidgetFocusProvider from './contexts/WidgetFocusProvider';
import WidgetFocusContext from './contexts/WidgetFocusContext';

jest.mock('views/stores/ViewMetadataStore', () => ({
  ViewMetadataStore: MockStore(
    ['listen', () => jest.fn()],
    'get',
    ['getInitialState', () => ({
      activeQuery: 'beef-dead',
    })],
  ),
}));

const mockedQueryIds = Immutable.List(['query-id-1', 'query-id-2']);

jest.mock('views/stores/QueryIdsStore', () => ({
  QueryIdsStore: {
    listen: () => jest.fn(),
    getInitialState: () => mockedQueryIds,
  },
}));

const mockedQueryTitles = Immutable.Map({
  'query-id-1': 'First dashboard page',
  'query-id-2': 'Second dashboard page',
});

jest.mock('views/stores/QueryTitlesStore', () => ({
  QueryTitlesStore: {
    listen: () => jest.fn(),
    getInitialState: () => mockedQueryTitles,
  },
}));

jest.mock('views/components/SearchResult', () => () => <div>Mocked search results</div>);
jest.mock('views/components/DashboardSearchBar', () => () => <div>Mocked dashboard search bar</div>);
jest.mock('components/layout/Footer', () => mockComponent('Footer'));
jest.mock('views/components/contexts/WidgetFocusProvider', () => jest.fn());

const mockWidgetEditing = () => {
  asMock(WidgetFocusProvider as React.FunctionComponent).mockImplementation(({ children }) => (
    <WidgetFocusContext.Provider value={{
      focusedWidget: {
        id: 'widget-id',
        editing: true,
        focusing: true,
      },
      setWidgetFocusing: () => {},
      setWidgetEditing: () => {},
      unsetWidgetFocusing: () => {},
      unsetWidgetEditing: () => {},
    }}>
      {children}
    </WidgetFocusContext.Provider>
  ));
};

describe('Dashboard Search', () => {
  beforeEach(() => {
    WidgetStore.listen = jest.fn(() => jest.fn());
    QueryFiltersStore.listen = jest.fn(() => jest.fn());
    // @ts-ignore
    SearchActions.execute = jest.fn(() => jest.fn());
    StreamsActions.refresh = mockAction(jest.fn());
    SearchConfigActions.refresh = mockAction(jest.fn());

    ViewStore.getInitialState = jest.fn(() => ({
      view: View.create().toBuilder().type(View.Type.Dashboard).build(),
      dirty: false,
      isNew: true,
      activeQuery: 'foobar',
    }));

    FieldTypesActions.all = mockAction(jest.fn(async () => {}));
    SearchMetadataStore.listen = jest.fn(() => jest.fn());
    SearchActions.refresh = mockAction(jest.fn(() => Promise.resolve()));

    asMock(WidgetFocusProvider as React.FunctionComponent).mockImplementation(({ children }) => (
      <WidgetFocusContext.Provider value={{
        focusedWidget: undefined,
        setWidgetFocusing: () => {},
        setWidgetEditing: () => {},
        unsetWidgetFocusing: () => {},
        unsetWidgetEditing: () => {},
      }}>
        {children}
      </WidgetFocusContext.Provider>
    ));
  });

  const SimpleSearch = (props) => (
    <Search location={{ query: {}, pathname: '/search', search: '' }}
            searchRefreshHooks={[]}
            {...props} />
  );

  it('should list tabs for dashboard pages', async () => {
    render(<SimpleSearch />);

    await waitFor(() => expect(screen.getByText('First dashboard page')).toBeInTheDocument());

    expect(screen.getByText('Second dashboard page')).toBeInTheDocument();
  });

  it('should not list tabs for pages when focusing a widget', async () => {
    mockWidgetEditing();
    render(<SimpleSearch />);

    await waitFor(() => expect(screen.getByText('Mocked search results')).toBeInTheDocument());

    expect(screen.queryByText('First dashboard page')).not.toBeInTheDocument();
    expect(screen.queryByText('Second dashboard page')).not.toBeInTheDocument();
  });

  it('should display dashboard search bar', async () => {
    render(<SimpleSearch />);

    await waitFor(() => expect(screen.getByText('Mocked dashboard search bar')).toBeInTheDocument());
  });

  it('should not display dashboard search bar on widget edit', async () => {
    mockWidgetEditing();
    render(<SimpleSearch />);

    await waitFor(() => expect(screen.getByText('Mocked search results')).toBeInTheDocument());

    expect(screen.queryByText('Mocked dashboard search bar')).not.toBeInTheDocument();
  });
});
