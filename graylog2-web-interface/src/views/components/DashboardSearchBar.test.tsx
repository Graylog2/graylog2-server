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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';

import mockSearchesClusterConfig from 'fixtures/searchClusterConfig';
import MockStore from 'helpers/mocking/StoreMock';
import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';
import { SearchActions } from 'views/stores/SearchStore';
import type { WidgetEditingState, WidgetFocusingState } from 'views/components/contexts/WidgetFocusContext';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

import DashboardSearchBar from './DashboardSearchBar';

jest.mock('views/components/searchbar/queryinput/QueryInput', () => ({ value = '' }: { value: string }) => <span>{value}</span>);
jest.mock('views/components/DashboardActionsMenu', () => () => <span>View Actions</span>);

jest.mock('views/stores/GlobalOverrideStore', () => ({
  GlobalOverrideStore: MockStore(),
  GlobalOverrideActions: {
    set: jest.fn().mockResolvedValue({}),
  },
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchStore: MockStore(['getInitialState', () => ({ search: { parameters: [] } })]),
  SearchActions: {
    refresh: jest.fn(),
  },
}));

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigStore: MockStore(['getInitialState', () => ({ searchesClusterConfig: mockSearchesClusterConfig })]),
  SearchConfigActions: {
    refresh: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/components/searchbar/queryvalidation/validateQuery', () => () => Promise.resolve({
  status: 'OK',
  explanations: [],
}));

jest.mock('views/logic/debounceWithPromise', () => (fn: any) => fn);

describe('DashboardSearchBar', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render the DashboardSearchBar', async () => {
    render(<DashboardSearchBar />);

    await screen.findByLabelText('Open Time Range Selector');
    await screen.findByLabelText('Search Time Range, Opens Time Range Selector On Click');
    await screen.findByLabelText('Refresh Search Controls');
    await screen.findByRole('button', { name: /perform search/i });
  });

  it('defaults to no override being selected', async () => {
    render(<DashboardSearchBar />);

    await screen.findByText('No Override');
  });

  it('should call SearchActions.refresh on submit when there are no changes', async () => {
    render(<DashboardSearchBar />);

    const searchButton = await screen.findByRole('button', { name: /perform search/i });

    await waitFor(() => expect(searchButton.classList).not.toContain('disabled'));

    userEvent.click(searchButton);

    await waitFor(() => expect(SearchActions.refresh).toHaveBeenCalledTimes(1));
  });

  it('should call SearchActions.refresh and set global override on submit when there are changes', async () => {
    render(<DashboardSearchBar />);

    const timeRangeInput = await screen.findByText(/no override/i);

    userEvent.click(timeRangeInput);
    userEvent.click(await screen.findByRole('tab', { name: 'Relative' }));
    userEvent.click(await screen.findByRole('button', { name: 'Update time range' }));

    const searchButton = await screen.findByRole('button', {
      name: /perform search \(changes were made after last search execution\)/i,
    });

    await waitFor(() => expect(searchButton.classList).not.toContain('disabled'));

    userEvent.click(searchButton);

    await waitFor(() => expect(GlobalOverrideActions.set).toHaveBeenCalledWith({ type: 'relative', from: 300 }, ''));
    await waitFor(() => expect(SearchActions.refresh).toHaveBeenCalledTimes(1));
  }, applyTimeoutMultiplier(10000));

  it('should hide the save and load controls if a widget is being edited', async () => {
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
        <DashboardSearchBar />
      </WidgetFocusContext.Provider>,
    );

    await screen.findByText('Not updating');

    const saveBtn = screen.queryByText('View Actions');

    expect(saveBtn).toBeNull();
  });

  it('should show the save and load controls if a widget is not being edited', async () => {
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
        <DashboardSearchBar />
      </WidgetFocusContext.Provider>,
    );

    await screen.findByText('View Actions');
  });
});
