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

import asMock from 'helpers/mocking/AsMock';
import mockComponent from 'helpers/mocking/MockComponent';
import mockAction from 'helpers/mocking/MockAction';
import { StreamsActions } from 'views/stores/StreamsStore';
import View from 'views/logic/views/View';
import Query, { filtersForQuery } from 'views/logic/queries/Query';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import useQueryIds from 'views/hooks/useQueryIds';
import useQueryTitles from 'views/hooks/useQueryTitles';
import { createSearch } from 'fixtures/searches';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';

import OriginalSearch from './Search';
import WidgetFocusProvider from './contexts/WidgetFocusProvider';
import WidgetFocusContext from './contexts/WidgetFocusContext';

jest.mock('hooks/useHotkey', () => jest.fn());
jest.mock('views/logic/fieldtypes/useFieldTypes');
jest.mock('hooks/useElementDimensions', () => () => ({ width: 1024, height: 768 }));

const mockedQueryIds = Immutable.OrderedSet(['query-id-1', 'query-id-2']);

jest.mock('views/hooks/useQueryIds');

const mockedQueryTitles = Immutable.Map({
  'query-id-1': 'First dashboard page',
  'query-id-2': 'Second dashboard page',
});

jest.mock('views/components/SearchResult', () => () => <div>Mocked search results</div>);
jest.mock('views/components/DashboardSearchBar', () => () => <div>Mocked dashboard search bar</div>);
jest.mock('components/layout/Footer', () => mockComponent('Footer'));
jest.mock('views/components/contexts/WidgetFocusProvider', () => jest.fn());
jest.mock('views/logic/queries/useCurrentQuery');
jest.mock('views/hooks/useQueryTitles');

const mockWidgetEditing = () => {
  asMock(WidgetFocusProvider as React.FunctionComponent).mockImplementation(({ children }: React.PropsWithChildren<{}>) => (
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

const defaultView = createSearch()
  .toBuilder()
  .type(View.Type.Dashboard)
  .build();

const Search = () => (
  <TestStoreProvider view={defaultView}>
    <OriginalSearch />
  </TestStoreProvider>
);

describe('Dashboard Search', () => {
  beforeEach(() => {
    StreamsActions.refresh = mockAction();

    asMock(WidgetFocusProvider as React.FunctionComponent).mockImplementation(({ children }: React.PropsWithChildren<{}>) => (
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
    asMock(useQueryIds).mockReturnValue(mockedQueryIds);
    asMock(useQueryTitles).mockReturnValue(mockedQueryTitles);
  });

  useViewsPlugin();

  it('should list tabs for dashboard pages', async () => {
    render(<Search />);

    await screen.findByRole('button', { name: /first dashboard page/i });
    await screen.findByRole('button', { name: /second dashboard page/i });
  });

  it('should not list tabs for pages when focusing a widget', async () => {
    mockWidgetEditing();
    render(<Search />);

    await screen.findByText('Mocked search results');

    expect(screen.queryByText('First dashboard page')).not.toBeInTheDocument();
    expect(screen.queryByText('Second dashboard page')).not.toBeInTheDocument();
  });

  it('should display dashboard search bar', async () => {
    render(<Search />);

    await screen.findByText('Mocked dashboard search bar');
  });

  it('should not display dashboard search bar on widget edit', async () => {
    mockWidgetEditing();
    render(<Search />);

    await screen.findByText('Mocked search results');

    expect(screen.queryByText('Mocked dashboard search bar')).not.toBeInTheDocument();
  });
});
