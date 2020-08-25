// @flow strict
import React from 'react';
import { render, fireEvent } from 'wrappedTestingLibrary';
import { viewsManager } from 'fixtures/users';

import type { UserJSON } from 'logic/users/User';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import CurrentUserContext from 'contexts/CurrentUserContext';

import ViewActionsMenu from './ViewActionsMenu';

const mockView = View.create().toBuilder().id('view-id').type(View.Type.Dashboard)
  .search(Search.builder().build())
  .title('View title')
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

jest.mock('views/stores/SearchExecutionStateStore', () => ({
  SearchExecutionStateStore: {
    getInitialState: jest.fn(),
    listen: () => jest.fn(),
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

jest.mock('stores/permissions/EntityShareStore', () => ({
  EntityShareActions: {
    prepare: jest.fn(() => Promise.resolve()),
    update: jest.fn(() => Promise.resolve()),
  },
  EntityShareStore: {
    listen: jest.fn(),
    getInitialState: jest.fn(() => ({ state: undefined })),
  },
}));

describe('ViewActionsMenu', () => {
  const SimpleViewActionMenu = ({ currentUser, ...props }: {currentUser?: UserJSON}) => (
    <CurrentUserContext.Provider value={currentUser}>
      <ViewActionsMenu {...props} router={{}} />
    </CurrentUserContext.Provider>
  );

  SimpleViewActionMenu.defaultProps = {
    currentUser: viewsManager,
  };

  it('should open modal to save new dashboard', () => {
    const { getByTestId, getByText } = render(<SimpleViewActionMenu />);
    const saveAsMenuItem = getByTestId('dashboard-save-as-button');

    fireEvent.click(saveAsMenuItem);

    expect(getByText('Save new dashboard')).not.toBeNull();
  });

  it('should open edit dashboard meta information modal', () => {
    const { getByText } = render(<SimpleViewActionMenu />);
    const editMenuItem = getByText(/Edit/i);

    fireEvent.click(editMenuItem);

    expect(getByText('Editing dashboard')).not.toBeNull();
  });

  it('should dashboard share modal', () => {
    const { getByText } = render(<SimpleViewActionMenu />);
    const openShareButton = getByText(/Share/i);

    fireEvent.click(openShareButton);

    expect(getByText(/Sharing/i)).not.toBeNull();
  });
});
