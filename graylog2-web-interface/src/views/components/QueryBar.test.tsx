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
import { fireEvent, render, screen, waitFor, within } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import OriginalQueryBar from 'views/components/QueryBar';
import DashboardPageContext from 'views/components/contexts/DashboardPageContext';
import useQueryIds from 'views/hooks/useQueryIds';
import useQueryTitles from 'views/hooks/useQueryTitles';
import useViewMetadata from 'views/hooks/useViewMetadata';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useAppDispatch from 'stores/useAppDispatch';
import { selectQuery, removeQuery } from 'views/logic/slices/viewSlice';

jest.mock('hooks/useElementDimensions', () => () => ({ width: 1024, height: 768 }));
jest.mock('views/logic/queries/useCurrentQueryId', () => () => 'bar');

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

jest.mock('views/hooks/useQueryIds');
jest.mock('views/hooks/useQueryTitles');
jest.mock('views/hooks/useViewMetadata');
jest.mock('stores/useAppDispatch');

jest.mock('views/logic/slices/viewSlice', () => ({
  ...jest.requireActual('views/logic/slices/viewSlice'),
  removeQuery: jest.fn(() => async () => {}),
  selectQuery: jest.fn(() => async () => {}),
}));

const QueryBar = () => (
  <TestStoreProvider>
    <OriginalQueryBar />
  </TestStoreProvider>
);

describe('QueryBar', () => {
  let oldWindowConfirm;

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    oldWindowConfirm = window.confirm;
    window.confirm = jest.fn(() => true);

    asMock(useQueryIds).mockReturnValue(queries);
    asMock(useQueryTitles).mockReturnValue(queryTitles);
    asMock(useViewMetadata).mockReturnValue(viewMetadata);
  });

  afterEach(() => {
    window.confirm = oldWindowConfirm;
  });

  it('renders existing tabs', async () => {
    render(<QueryBar />);

    await screen.findByRole('button', { name: 'First Query' });
    await screen.findByRole('button', { name: 'Second Query' });
    await screen.findByRole('button', { name: 'Third Query' });
  });

  it('allows changing tab', async () => {
    const dispatch = jest.fn();
    asMock(useAppDispatch).mockReturnValue(dispatch);

    render(<QueryBar />);

    const nextTab = await screen.findByRole('button', { name: 'Third Query' });

    fireEvent.click(nextTab);

    await waitFor(() => expect(selectQuery).toHaveBeenCalledWith('baz'));
  });

  it('allows closing current tab', async () => {
    const dispatch = jest.fn();
    asMock(useAppDispatch).mockReturnValue(dispatch);
    const setDashboard = jest.fn();

    render(
      <DashboardPageContext.Provider value={{
        setDashboardPage: setDashboard,
        unsetDashboardPage: jest.fn(),
        dashboardPage: undefined,
      }}>
        <QueryBar />
      </DashboardPageContext.Provider>,
    );

    const currentTab = await screen.findByRole('button', { name: 'Second Query' });

    const dropdown = await within(currentTab).findByTestId('query-action-dropdown');

    fireEvent.click(dropdown);

    const closeButton = await screen.findByRole('menuitem', { name: 'Delete', hidden: true });

    fireEvent.click(closeButton);

    await waitFor(() => expect(removeQuery).toHaveBeenCalledWith('bar'));

    expect(window.confirm).toHaveBeenCalled();
  });
});
