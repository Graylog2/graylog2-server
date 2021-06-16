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
import { MockStore } from 'helpers/mocking';

import QueryBar from 'views/components/QueryBar';
import { ViewActions } from 'views/stores/ViewStore';

jest.mock('react-sizeme', () => ({
  SizeMe: ({ children: fn }) => fn({ size: { width: 1024, height: 768 } }),
}));

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
  ViewStatesStore: MockStore(['getInitialState', () => new Map()]),
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
    render(<QueryBar queries={queries} queryTitles={queryTitles} viewMetadata={viewMetadata} />);

    const currentTab = await screen.findByRole('button', { name: 'Second Query' });

    const dropdown = await within(currentTab).findByTestId('query-action-dropdown');

    fireEvent.click(dropdown);

    const closeButton = await screen.findByRole('menuitem', { name: 'Close' });

    fireEvent.click(closeButton);

    await waitFor(() => expect(ViewActions.selectQuery).toHaveBeenCalledWith('foo'));
    await waitFor(() => expect(ViewActions.search).toHaveBeenCalled());
  });
});
