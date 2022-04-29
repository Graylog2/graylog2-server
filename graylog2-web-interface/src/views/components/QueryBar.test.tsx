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
import * as Immutable from 'immutable';
import { Map as MockMap } from 'immutable';
import { fireEvent, render, screen, waitFor, within } from 'wrappedTestingLibrary';

import { MockStore } from 'helpers/mocking';
import QueryBar from 'views/components/QueryBar';
import { ViewActions } from 'views/stores/ViewStore';
import DashboardPageContext from 'views/components/contexts/DashboardPageContext';

jest.mock('hooks/useElementDimensions', () => () => ({ width: 1024, height: 768 }));

jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {
    selectQuery: jest.fn(() => Promise.resolve()),
    search: jest.fn(() => Promise.resolve()),
  },
  ViewStore: MockStore(['getInitialState', () => ({ view: {} })]),
}));

jest.mock('views/stores/ViewStatesStore', () => ({
  ViewStatesActions: {
    remove: jest.fn(() => Promise.resolve()),
  },
  ViewStatesStore: MockStore(['getInitialState', () => MockMap()]),
}));

const queries = Immutable.OrderedSet(['foo', 'bar', 'baz']);
const queryTitles = Immutable.Map({
  foo: 'First Query',
  bar: 'Second Query',
  baz: 'Third Query',
});

const viewMetadata = {
  id: 'viewId',
  title: 'Some view',
  description: 'Hey There!',
  summary: 'Very helpful summary',
  activeQuery: 'bar',
};

describe('QueryBar', () => {
  let oldWindowConfirm;

  beforeEach(() => {
    oldWindowConfirm = window.confirm;
    window.confirm = jest.fn(() => true);
  });

  afterEach(() => {
    window.confirm = oldWindowConfirm;
  });

  it('renders existing tabs', async () => {
    render(<QueryBar queries={queries} queryTitles={queryTitles} viewMetadata={viewMetadata} />);

    await screen.findByRole('button', { name: 'First Query' });
    await screen.findByRole('button', { name: 'Second Query' });
    await screen.findByRole('button', { name: 'Third Query' });
  });

  it('allows changing tab', async () => {
    render(<QueryBar queries={queries} queryTitles={queryTitles} viewMetadata={viewMetadata} />);

    const nextTab = await screen.findByRole('button', { name: 'Third Query' });

    fireEvent.click(nextTab);

    await waitFor(() => expect(ViewActions.selectQuery).toHaveBeenCalledWith('baz'));
  });

  it('allows closing current tab', async () => {
    const setDashboard = jest.fn();

    render(
      <DashboardPageContext.Provider value={{
        setDashboardPage: setDashboard,
        unsetDashboardPage: jest.fn(),
        dashboardPage: undefined,
      }}>
        <QueryBar queries={queries} queryTitles={queryTitles} viewMetadata={viewMetadata} />
      </DashboardPageContext.Provider>,
    );

    const currentTab = await screen.findByRole('button', { name: 'Second Query' });

    const dropdown = await within(currentTab).findByTestId('query-action-dropdown');

    fireEvent.click(dropdown);

    const closeButton = await screen.findByRole('menuitem', { name: 'Delete' });

    fireEvent.click(closeButton);

    await waitFor(() => expect(setDashboard).toHaveBeenCalled());
    await waitFor(() => expect(ViewActions.search).toHaveBeenCalled());

    expect(window.confirm).toHaveBeenCalled();
  });
});
