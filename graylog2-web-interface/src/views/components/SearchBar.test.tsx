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
import mockAction from 'helpers/mocking/MockAction';
import { SearchActions } from 'views/stores/SearchStore';
import MockQuery from 'views/logic/queries/Query';
import type { WidgetEditingState, WidgetFocusingState } from 'views/components/contexts/WidgetFocusContext';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';
import mockSearchesClusterConfig from 'fixtures/searchClusterConfig';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';

import SearchBar from './SearchBar';

const mockCurrentUser = { currentUser: { fullname: 'Ada Lovelace', username: 'ada' } };

jest.mock('views/stores/SearchStore', () => ({
  SearchStore: MockStore(
    ['getInitialState', () => ({ search: { parameters: [] } })],
  ),
  SearchActions: {
    refresh: jest.fn(),
  },
}));

jest.mock('stores/users/CurrentUserStore', () => ({
  CurrentUserStore: MockStore(
    ['get', () => mockCurrentUser],
    ['getInitialState', () => mockCurrentUser],
  ),
}));

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

jest.mock('views/components/searchbar/saved-search/SavedSearchControls', () => jest.fn(() => (
  <div>Saved Search Controls</div>
)));

jest.mock('views/stores/CurrentQueryStore', () => ({
  CurrentQueryStore: MockStore(['getInitialState', () => MockQuery.builder()
    .timerange({ type: 'relative', from: 300 })
    .query({ type: 'elasticsearch', query_string: '*' })
    .id('34efae1e-e78e-48ab-ab3f-e83c8611a683')
    .build()]),
}));

jest.mock('views/components/searchbar/queryvalidation/validateQuery', () => jest.fn(() => Promise.resolve({
  status: 'OK',
  explanations: [],
})));

jest.mock('views/logic/debounceWithPromise', () => (fn: any) => fn);

describe('SearchBar', () => {
  beforeEach(() => {
    SearchActions.refresh = mockAction();
    SearchConfigStore.getInitialState = jest.fn(() => ({ searchesClusterConfig: mockSearchesClusterConfig }));
  });

  it('should render the SearchBar', async () => {
    render(<SearchBar />);

    const timeRangeButton = await screen.findByLabelText('Open Time Range Selector');
    const timeRangeDisplay = await screen.findByLabelText('Search Time Range, Opens Time Range Selector On Click');
    const streamsFilter = await screen.findByTestId('streams-filter');
    const liveUpdate = await screen.findByLabelText('Refresh Search Controls');
    const searchButton = await screen.findByRole('button', { name: /perform search/i });
    const metaButtons = await screen.findByText('Saved Search Controls');

    expect(timeRangeButton).not.toBeNull();
    expect(timeRangeDisplay).not.toBeNull();
    expect(streamsFilter).not.toBeNull();
    expect(liveUpdate).not.toBeNull();
    expect(searchButton).not.toBeNull();
    expect(metaButtons).not.toBeNull();
  });

  it('should refresh search, when search is performed and there are no changes.', async () => {
    render(<SearchBar />);

    const searchButton = await screen.findByRole('button', { name: /perform search/i });

    await waitFor(() => expect(searchButton.classList).not.toContain('disabled'));

    fireEvent.click(searchButton);

    await waitFor(() => expect(SearchActions.refresh).toHaveBeenCalledTimes(1));
  });

  it('date exceeding limitDuration should render with error Icon & search button disabled', async () => {
    asMock(SearchConfigStore.getInitialState).mockReturnValue({ searchesClusterConfig: { ...mockSearchesClusterConfig, query_time_range_limit: 'PT1M' } });
    render(<SearchBar />);

    const timeRangeButton = await screen.findByLabelText('Open Time Range Selector');
    const searchButton = await screen.findByRole('button', { name: /perform search/i });

    await waitFor(() => expect(searchButton.classList).toContain('disabled'));
    await waitFor(() => expect(timeRangeButton.firstChild).toHaveClass('fa-exclamation-triangle'));
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
