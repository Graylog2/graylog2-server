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
import { render, screen } from 'wrappedTestingLibrary';

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
import { SearchMetadataStore } from 'views/stores/SearchMetadataStore';
import View from 'views/logic/views/View';
import type { SearchExecutionResult } from 'views/actions/SearchActions';
import Query, { filtersForQuery } from 'views/logic/queries/Query';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import { SearchPageLayoutProvider } from 'views/components/contexts/SearchPageLayoutContext';

import Search from './Search';
import WidgetFocusProvider from './contexts/WidgetFocusProvider';
import WidgetFocusContext from './contexts/WidgetFocusContext';

jest.mock('views/stores/ViewMetadataStore', () => ({
  ViewMetadataStore: MockStore(
    'get',
    ['getInitialState', () => ({
      activeQuery: 'beef-dead',
    })],
  ),
}));

jest.mock('hooks/useElementDimensions', () => () => ({ width: 1024, height: 768 }));

const mockedQueryIds = Immutable.OrderedSet(['query-id-1', 'query-id-2']);

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
jest.mock('views/logic/queries/useCurrentQuery');

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
  const SUT = (props) => (
    <SearchPageLayoutProvider>
      <Search {...props} />
    </SearchPageLayoutProvider>
  );

  beforeEach(() => {
    WidgetStore.listen = jest.fn(() => jest.fn());
    QueryFiltersStore.listen = jest.fn(() => jest.fn());
    SearchActions.execute = mockAction(jest.fn(() => Promise.resolve({} as SearchExecutionResult)));
    StreamsActions.refresh = mockAction();
    SearchConfigActions.refresh = mockAction();

    ViewStore.getInitialState = jest.fn(() => ({
      view: View.create().toBuilder().type(View.Type.Dashboard).build(),
      dirty: false,
      isNew: true,
      activeQuery: 'foobar',
    }));

    SearchMetadataStore.listen = jest.fn(() => jest.fn());
    SearchActions.refresh = mockAction();

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

    const query = Query.builder().id('foobar').filter(filtersForQuery([])).build();
    asMock(useCurrentQuery).mockReturnValue(query);
  });

  it('should list tabs for dashboard pages', async () => {
    render(<SUT />);

    await screen.findByRole('button', { name: 'First dashboard page' });

    expect(screen.getByRole('button', { name: 'Second dashboard page' })).toBeInTheDocument();
  });

  it('should not list tabs for pages when focusing a widget', async () => {
    mockWidgetEditing();
    render(<SUT />);

    await screen.findByText('Mocked search results');

    expect(screen.queryByText('First dashboard page')).not.toBeInTheDocument();
    expect(screen.queryByText('Second dashboard page')).not.toBeInTheDocument();
  });

  it('should display dashboard search bar', async () => {
    render(<SUT />);

    await screen.findByText('Mocked dashboard search bar');
  });

  it('should not display dashboard search bar on widget edit', async () => {
    mockWidgetEditing();
    render(<SUT />);

    await screen.findByText('Mocked search results');

    expect(screen.queryByText('Mocked dashboard search bar')).not.toBeInTheDocument();
  });
});
