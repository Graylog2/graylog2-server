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
import { render, fireEvent } from 'wrappedTestingLibrary';

import { alice } from 'fixtures/users';
import type User from 'logic/users/User';
import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import CurrentUserContext from 'contexts/CurrentUserContext';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import { ViewActionsLayoutOptions } from 'views/components/contexts/SearchPageLayoutContext';

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

  const SimpleViewActionMenu = ({ currentUser: user, providerOverrides, ...props }: {currentUser?: User, providerOverrides?: LayoutState}) => (
    <SearchPageLayoutProvider providerOverrides={providerOverrides}>
      <CurrentUserContext.Provider value={user}>
        <ViewActionsMenu {...props} />
      </CurrentUserContext.Provider>
    </SearchPageLayoutProvider>
  );

  SimpleViewActionMenu.defaultProps = {
    currentUser,
    providerOverrides: undefined,
  };

  it('should open modal to save new dashboard', () => {
    const { getByTitle, getByText } = render(<SimpleViewActionMenu />);
    const saveAsMenuItem = getByTitle(/Save As Button/);

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

  it('should use FULL_MENU layout option by default and render all buttons', async () => {
    const { findByRole, findByTitle } = render(<SimpleViewActionMenu />);

    await findByTitle(/Save Button/);
    await findByTitle(/Save As Button/);
    await findByTitle(/Share/);
    await findByRole(/^menu$/);
  });

  it('should only render "Save As" button in SAVE_COPY layout option', async () => {
    const { findByTitle, queryByRole, queryByTitle } = render(<SimpleViewActionMenu providerOverrides={{ sidebar: { isShown: false }, viewActionsLayoutOptions: ViewActionsLayoutOptions.SAVE_COPY }} />);

    const saveButton = queryByTitle(/Save Button/);
    const shareButton = queryByTitle(/Share/);
    const extrasButton = queryByRole(/^menu$/);

    expect(saveButton).not.toBeInTheDocument();
    expect(shareButton).not.toBeInTheDocument();
    expect(extrasButton).not.toBeInTheDocument();

    await findByTitle(/Save As Button/);
  });

  it('should render no action menu items in BLANK layout option', () => {
    const { queryByRole, queryByTitle } = render(<SimpleViewActionMenu providerOverrides={{ sidebar: { isShown: false }, viewActionsLayoutOptions: ViewActionsLayoutOptions.BLANK }} />);

    const saveButton = queryByTitle(/Save Button/);
    const saveAsButton = queryByTitle(/Save As Button/);
    const shareButton = queryByTitle(/Share/);
    const extrasButton = queryByRole(/^menu$/);

    expect(saveButton).not.toBeInTheDocument();
    expect(saveAsButton).not.toBeInTheDocument();
    expect(shareButton).not.toBeInTheDocument();
    expect(extrasButton).not.toBeInTheDocument();
  });
});
