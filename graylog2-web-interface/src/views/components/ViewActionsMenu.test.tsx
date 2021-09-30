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
import React from 'react';
import * as mockImmutable from 'immutable';
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import { alice } from 'fixtures/users';

import User from 'logic/users/User';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import CurrentUserContext from 'contexts/CurrentUserContext';

import ViewActionsMenu from './ViewActionsMenu';

const mockView = View.create().toBuilder().id('view-id').type(View.Type.Dashboard)
  .search(Search.builder().build())
  .title('View title')
  .createdAt(new Date('2019-10-16T14:38:44.681Z'))
  .build();

jest.mock('views/stores/ViewStore', () => ({
  ViewStore: {
    getInitialState: () => ({ isNew: false, view: mockView }),
    listen: () => jest.fn(),
  },
}));

jest.mock('views/stores/SearchMetadataStore', () => ({
  SearchMetadataStore: {
    getInitialState: () => ({ undeclared: mockImmutable.Set() }),
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
  const currentUser = alice.toBuilder()
    .grnPermissions(mockImmutable.List(['entity:own:grn::::dashboard:view-id']))
    .permissions(mockImmutable.List(['dashboards:edit:view-id', 'view:edit:view-id']))
    .build();

  const SimpleViewActionMenu = ({ currentUser: user, ...props }: {currentUser?: User}) => (
    <CurrentUserContext.Provider value={user}>
      <ViewActionsMenu {...props} />
    </CurrentUserContext.Provider>
  );

  SimpleViewActionMenu.defaultProps = {
    currentUser,
  };

  it('should open modal to save new dashboard', () => {
    const { getByTestId, getByText } = render(<SimpleViewActionMenu />);
    const saveAsMenuItem = getByTestId('dashboard-save-as-button');

    fireEvent.click(saveAsMenuItem);

    expect(getByText('Save new dashboard')).not.toBeNull();
  });

  it('should open edit dashboard meta information modal', async () => {
    const { getByText, findByText } = render(<SimpleViewActionMenu />);
    const editMenuItem = getByText(/Edit metadata/i);

    fireEvent.click(editMenuItem);

    await findByText(/Editing dashboard/);
  });

  it('should dashboard share modal', () => {
    const { getByText } = render(<SimpleViewActionMenu />);
    const openShareButton = getByText(/Share/i);

    fireEvent.click(openShareButton);

    expect(getByText(/Sharing/i)).not.toBeNull();
  });
});
