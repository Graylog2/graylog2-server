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
import { act, render, screen, fireEvent, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import MockStore from 'helpers/mocking/StoreMock';

import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';
import { SearchActions } from 'views/stores/SearchStore';
import WidgetFocusContext, {
  WidgetEditingState,
  WidgetFocusingState
} from 'views/components/contexts/WidgetFocusContext';

import DashboardSearchBar from './DashboardSearchBar';

jest.mock('views/components/ViewActionsMenu', () => () => <span>View Actions</span>);

jest.mock('views/stores/GlobalOverrideStore', () => ({
  GlobalOverrideStore: MockStore(),
  GlobalOverrideActions: {
    set: jest.fn().mockResolvedValue({}),
  },
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchActions: {
    refresh: jest.fn(),
  },
}));

jest.mock('views/components/searchbar/AsyncQueryInput', () => () => null);

const config = {
  analysis_disabled_fields: ['full_message', 'message'],
  query_time_range_limit: 'PT0S',
  relative_timerange_options: { PT0S: 'Search in all messages', P5m: 'Search in last five minutes' },
  surrounding_filter_fields: ['file', 'source', 'gl2_source_input', 'source_file'],
  surrounding_timerange_options: { PT1S: 'One second', PT2S: 'Two seconds' },
};

describe('DashboardSearchBar', () => {
  const onExecute = jest.fn();

  it('should render the DashboardSearchBar', () => {
    render(<DashboardSearchBar onExecute={onExecute} config={config} />);

    const timeRangeButton = screen.getByLabelText('Open Time Range Selector');
    const timeRangeDisplay = screen.getByLabelText('Search Time Range, Opens Time Range Selector On Click');
    const liveUpdate = screen.getByLabelText('Refresh Search Controls');
    const searchButton = screen.getByTitle('Perform search');

    expect(timeRangeButton).not.toBeNull();
    expect(timeRangeDisplay).not.toBeNull();
    expect(liveUpdate).not.toBeNull();
    expect(searchButton).not.toBeNull();
  });

  it('defaults to no override being selected', () => {
    render(<DashboardSearchBar onExecute={onExecute} config={config} />);

    expect(screen.getByText('No Override')).toBeVisible();
  });

  it('should refresh search when button is clicked', async () => {
    render(<DashboardSearchBar onExecute={onExecute} config={config} />);

    const searchButton = screen.getByTitle('Perform search');

    fireEvent.click(searchButton);

    await waitFor(() => expect(SearchActions.refresh).toHaveBeenCalledTimes(1));
  });

  it('should call onExecute and set global override when search is performed', async () => {
    render(<DashboardSearchBar onExecute={onExecute} config={config} />);

    const timeRangeInput = await screen.findByText(/no override/i);

    act(() => {
      userEvent.click(timeRangeInput);
    });

    userEvent.click(await screen.findByRole('tab', { name: 'Relative' }));
    userEvent.click(await screen.findByRole('button', { name: 'Apply' }));

    const searchButton = await screen.findByTitle('Perform search (changes were made after last search execution)');

    fireEvent.click(searchButton);

    await waitFor(() => expect(GlobalOverrideActions.set).toHaveBeenCalledWith({ type: 'relative', from: 300 }, ''));
  });

  it('should hide the save and load controls if a widget is being edited', () => {
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
        <DashboardSearchBar onExecute={onExecute} config={config} />
      </WidgetFocusContext.Provider>,
    );

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
        <DashboardSearchBar onExecute={onExecute} config={config} />
      </WidgetFocusContext.Provider>,
    );

    const saveBtn = await screen.findByText('View Actions');

    expect(saveBtn).not.toBeNull();
  });
});
