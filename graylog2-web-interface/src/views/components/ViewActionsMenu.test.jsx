// @flow strict
import React from 'react';
import { cleanup, render, fireEvent } from 'wrappedTestingLibrary';

import { StoreMock as MockStore } from 'helpers/mocking';

import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';

import ViewActionsMenu from './ViewActionsMenu';

const mockView = View.create().toBuilder().id('view-id').type(View.Type.Dashboard)
  .search(Search.builder().build())
  .createdAt(new Date('2019-10-16T14:38:44.681Z'))
  .build();

jest.mock('react-router', () => ({ withRouter: (x) => x }));
jest.mock('views/stores/ViewStore', () => ({
  ViewStore: {
    getInitialState: () => ({ isNew: false, view: mockView }),
    listen: () => jest.fn(),
  },
}));
jest.mock('views/stores/SearchMetadataStore', () => ({
  SearchMetadataStore: {
    getInitialState: () => ({ undeclared: [] }),
    listen: () => jest.fn(),
  },
}));
jest.mock('stores/users/CurrentUserStore', () => MockStore(
  ['listen', () => jest.fn()],
  'get',
  ['getInitialState', () => ({
    currentUser: {
      full_name: 'Betty Holberton',
      username: 'betty',
      permissions: ['dashboards:edit:view-id', 'view:edit:view-id'],
      roles: ['Views Manager'],
    },
  })],
));
jest.mock('views/stores/SearchStore', () => ({
  SearchActions: {
    execute: jest.fn(() => Promise.resolve()),
  },
  SearchStore: {
    listen: () => jest.fn(),
    getInitialState: () => ({
      result: {
        forId: jest.fn(() => ({})),
      },
      widgetMapping: {},
    }),
  },
}));

jest.mock('views/stores/ViewSharingStore', () => ({
  ViewSharingActions: {
    create: jest.fn(() => Promise.resolve()),
    get: jest.fn(() => Promise.resolve()),
    remove: jest.fn(() => Promise.resolve()),
    users: jest.fn(() => Promise.resolve()),
  },
}));

describe('ViewActionsMenu', () => {
  afterEach(cleanup);

  it('should open modal to save new dashboard', () => {
    const { getByTestId, getByText } = render(<ViewActionsMenu router={{}} />);
    const saveAsMenuItem = getByTestId('dashboard-save-as-button');
    fireEvent.click(saveAsMenuItem);

    expect(getByText('Save new dashboard')).not.toBeNull();
  });

  it('should open edit dashboard meta information modal', () => {
    const { getByText } = render(<ViewActionsMenu router={{}} />);
    const editMenuItem = getByText(/Edit/i);
    fireEvent.click(editMenuItem);

    expect(getByText('Editing dashboard')).not.toBeNull();
  });
  it('should dashboard share modal', () => {
    const { getByText } = render(<ViewActionsMenu router={{}} />);
    const editMenuItem = getByText(/Share/i);
    fireEvent.click(editMenuItem);

    expect(getByText(/Who is supposed to access the dashboard/i)).not.toBeNull();
  });
});
