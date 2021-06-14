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

  it('allows closing current tab', async () => {
    render(<QueryBar queries={queries} queryTitles={queryTitles} viewMetadata={viewMetadata} />);

    const currentTab = await screen.findByRole('button', { name: 'Second Query' });

    const dropdown = await within(currentTab).findByTestId('query-action-dropdown');

    fireEvent.click(dropdown);

    const closeButton = await screen.findByRole('menuitem', { name: 'Close' });

    fireEvent.click(closeButton);

    await waitFor(() => expect(ViewActions.selectQuery).toHaveBeenCalledWith('foo'));
  });
});
