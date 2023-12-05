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
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';

import { StoreMock as MockStore, asMock } from 'helpers/mocking';
import MockQuery from 'views/logic/queries/Query';
import type { WidgetEditingState, WidgetFocusingState } from 'views/components/contexts/WidgetFocusContext';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';
import mockSearchesClusterConfig from 'fixtures/searchClusterConfig';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useAppDispatch from 'stores/useAppDispatch';

import OriginalSearchBar from './SearchBar';

jest.mock('views/logic/fieldtypes/useFieldTypes');

jest.mock('stores/streams/StreamsStore', () => MockStore(
  ['listStreams', () => ({ then: jest.fn() })],
  'availableStreams',
));

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigStore: MockStore(),
  SearchConfigActions: {
    refresh: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/components/searchbar/saved-search/SearchActionsMenu', () => jest.fn(() => (
  <div>Saved Search Controls</div>
)));

jest.mock('views/components/searchbar/queryvalidation/validateQuery', () => jest.fn(() => Promise.resolve({
  status: 'OK',
  explanations: [],
})));

jest.mock('views/logic/debounceWithPromise', () => (fn: any) => fn);
jest.mock('views/logic/queries/useCurrentQuery');
jest.mock('stores/useAppDispatch');

jest.mock('views/hooks/useAutoRefresh', () => () => ({
  refreshConfig: null,
  startAutoRefresh: () => {},
  stopAutoRefresh: () => {},
}));

const query = MockQuery.builder()
  .timerange({ type: 'relative', from: 300 })
  .query({ type: 'elasticsearch', query_string: '*' })
  .id('34efae1e-e78e-48ab-ab3f-e83c8611a683')
  .build();

const SearchBar = () => (
  <TestStoreProvider>
    <OriginalSearchBar />
  </TestStoreProvider>
);

describe('SearchBar', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    SearchConfigStore.getInitialState = jest.fn(() => ({ searchesClusterConfig: mockSearchesClusterConfig }));

    asMock(useCurrentQuery).mockReturnValue(query);
  });

  it('should render the SearchBar', async () => {
    render(<SearchBar />);

    await screen.findByLabelText('Open Time Range Selector');
    await screen.findByLabelText('Search Time Range, Opens Time Range Selector On Click');
    await screen.findByTestId('streams-filter');
    await screen.findByLabelText('Refresh Search Controls');
    await screen.findByRole('button', { name: /perform search/i });
    await screen.findByText('Saved Search Controls');
  });

  it('should refresh search, when search is performed and there are no changes.', async () => {
    const dispatch = jest.fn();
    asMock(useAppDispatch).mockReturnValue(dispatch);

    render(<SearchBar />);

    const searchButton = await screen.findByRole('button', { name: /perform search/i });

    await waitFor(() => expect(searchButton.classList).not.toContain('disabled'));

    asMock(dispatch).mockClear();

    fireEvent.click(searchButton);

    await waitFor(() => expect(dispatch).toHaveBeenCalled());
  });

  it('date exceeding limitDuration should render with error Icon & search button disabled', async () => {
    asMock(SearchConfigStore.getInitialState).mockReturnValue({ searchesClusterConfig: { ...mockSearchesClusterConfig, query_time_range_limit: 'PT1M' } });
    render(<SearchBar />);

    const timeRangePickerButton = await screen.findByLabelText('Open Time Range Selector');
    const searchButton = await screen.findByRole('button', { name: /perform search/i });

    await waitFor(() => expect(searchButton.classList).toContain('disabled'));
    const exclamationIcon = timeRangePickerButton.querySelector('svg');
    await waitFor(() => expect(exclamationIcon).toHaveClass('fa-exclamation-triangle'));
  });

  it('should hide the save load controls if editing the widget', async () => {
    const focusedWidget: WidgetEditingState = { id: 'foo', editing: true, focusing: true };
    const widgetFocusContext = {
      focusedWidget,
      setWidgetFocusing: () => {},
      setWidgetEditing: () => {},
      unsetWidgetFocusing: () => {},
      unsetWidgetEditing: () => {},
    };

    render(
      <WidgetFocusContext.Provider value={widgetFocusContext}>
        <SearchBar />
      </WidgetFocusContext.Provider>,
    );

    await screen.findByRole('button', { name: /perform search/i });
    const saveBtn = screen.queryByText('Saved Search Controls');

    expect(saveBtn).toBeNull();
  });

  it('should show the save load controls if the widget is not edited', async () => {
    const focusedWidget: WidgetFocusingState = { id: 'foo', editing: false, focusing: true };
    const widgetFocusContext = {
      focusedWidget,
      setWidgetFocusing: () => {},
      setWidgetEditing: () => {},
      unsetWidgetFocusing: () => {},
      unsetWidgetEditing: () => {},
    };

    render(
      <WidgetFocusContext.Provider value={widgetFocusContext}>
        <SearchBar />
      </WidgetFocusContext.Provider>,
    );

    const saveBtn = await screen.findByText('Saved Search Controls');

    expect(saveBtn).not.toBeNull();
  });

  it('should validate query on mount', async () => {
    render(<SearchBar />);

    await waitFor(() => expect(validateQuery).toHaveBeenCalled());
  });
});
