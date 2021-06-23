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
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';
import { viewsManager, admin } from 'fixtures/users';
import mockAction from 'helpers/mocking/MockAction';
import userEvent from '@testing-library/user-event';

import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import ViewLoaderContext, { ViewLoaderContextType } from 'views/logic/ViewLoaderContext';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import NewViewLoaderContext, { NewViewLoaderContextType } from 'views/logic/NewViewLoaderContext';
import * as Permissions from 'views/Permissions';
import CurrentUserContext from 'contexts/CurrentUserContext';
import type { UserJSON } from 'logic/users/User';
import type { ViewStoreState } from 'views/stores/ViewStore';

import SavedSearchControls from './SavedSearchControls';

jest.mock('views/stores/FieldTypesStore', () => ({}));

describe('SavedSearchControls', () => {
  const createViewStoreState = (dirty = true, id = undefined) => ({
    activeQuery: '',
    view: View.builder()
      .id(id)
      .title('title')
      .type(View.Type.Search)
      .description('description')
      .search(Search.create().toBuilder().id('id-beef').build())
      .owner('owningUser')
      .build(),
    dirty,
    isNew: false,
  });

  type SimpleSavedSearchControlsProps = {
    loadNewView?: NewViewLoaderContextType,
    onLoadView?: ViewLoaderContextType,
    currentUser?: UserJSON,
    viewStoreState?: ViewStoreState,
  };

  const SimpleSavedSearchControls = ({ loadNewView = () => Promise.resolve(), onLoadView, currentUser, ...props }: SimpleSavedSearchControlsProps) => (
    <ViewLoaderContext.Provider value={onLoadView}>
      <CurrentUserContext.Provider value={currentUser}>
        <NewViewLoaderContext.Provider value={loadNewView}>
          <SavedSearchControls {...props} />
        </NewViewLoaderContext.Provider>
      </CurrentUserContext.Provider>
    </ViewLoaderContext.Provider>
  );

  const findShareButton = () => screen.findByRole('button', { name: 'Share' });

  SimpleSavedSearchControls.defaultProps = {
    loadNewView: () => Promise.resolve(),
    onLoadView: () => Promise.resolve(),
    currentUser: viewsManager,
    viewStoreState: createViewStoreState(),
  };

  describe('Button handling', () => {
    it('should clear a view', async () => {
      const loadNewView = jest.fn(() => Promise.resolve());

      render(<SimpleSavedSearchControls loadNewView={loadNewView} />);

      const resetSearch = await screen.findByTestId('reset-search');

      fireEvent.click(resetSearch);

      expect(loadNewView).toHaveBeenCalledTimes(1);
    });

    it('should loadView after create', async () => {
      ViewManagementActions.create = mockAction(jest.fn((view) => Promise.resolve(view)));
      const onLoadView = jest.fn((view) => new Promise(() => view));

      render(<SimpleSavedSearchControls onLoadView={onLoadView} viewStoreState={createViewStoreState(false)} />);

      fireEvent.click(await screen.findByTitle('Save search'));
      userEvent.type(await screen.findByPlaceholderText('Enter title'), 'Test');
      fireEvent.click(await screen.findByText('Create new'));

      await waitFor(() => expect(onLoadView).toHaveBeenCalledTimes(1));
    });

    describe('has "Share" option', () => {
      it('includes the option to share the current search', async () => {
        render(<SimpleSavedSearchControls viewStoreState={createViewStoreState(false, 'some-id')} />);

        await findShareButton();
      });

      it('which should be disabled if current user is neither owner nor permitted to edit search', async () => {
        const notOwningUser = {
          ...viewsManager,
          username: 'notOwningUser',
          permissions: [],
          grn_permissions: [],
        };
        render(<SimpleSavedSearchControls currentUser={notOwningUser} viewStoreState={createViewStoreState(false, 'some-id')} />);

        const shareButton = await findShareButton();

        expect(shareButton).toBeDisabled();
      });

      it('which should be enabled if current user is owner of search', async () => {
        const owningUser = {
          ...viewsManager,
          username: 'owningUser',
          permissions: [],
          grn_permissions: ['entity:own:grn::::search:user-id-1'],
        };

        render(<SimpleSavedSearchControls currentUser={owningUser} viewStoreState={createViewStoreState(false, owningUser.id)} />);

        const shareButton = await findShareButton();

        expect(shareButton).not.toBeDisabled();
      });

      it('which should be enabled if current user is permitted to edit search', async () => {
        const owningUser = {
          ...viewsManager,
          username: 'powerfulUser',
          permissions: [Permissions.View.Edit(viewsManager.id)],
          grn_permissions: ['entity:own:grn::::search:user-id-1'],
        };

        render(<SimpleSavedSearchControls currentUser={owningUser} viewStoreState={createViewStoreState(false, owningUser.id)} />);

        const shareButton = await findShareButton();

        expect(shareButton).not.toBeDisabled();
      });

      it('which should be enabled if current user is admin', async () => {
        render(<SimpleSavedSearchControls currentUser={admin} viewStoreState={createViewStoreState(false, admin.id)} />);

        const shareSearch = await findShareButton();

        expect(shareSearch).not.toBeDisabled();
      });

      it('which should be hidden if search is unsaved', async () => {
        render(<SimpleSavedSearchControls />);

        const shareSearch = await findShareButton();

        expect(shareSearch).toMatchSnapshot();
      });
    });
  });

  describe('render the SavedSearchControls', () => {
    it('should render not dirty with unsaved view', async () => {
      render(<SimpleSavedSearchControls viewStoreState={createViewStoreState(false)} />);

      await screen.findByRole('button', { name: 'Save search' });
    });

    it('should render not dirty', async () => {
      const viewStoreState = {
        activeQuery: '',
        view: View.builder()
          .title('title')
          .description('description')
          .type(View.Type.Search)
          .search(Search.create().toBuilder().id('id-beef').build())
          .id('id-beef')
          .build(),
        dirty: false,
        isNew: true,
      };
      render(<SimpleSavedSearchControls viewStoreState={viewStoreState} />);

      await screen.findByRole('button', { name: 'Saved search' });
    });

    it('should render dirty', async () => {
      const view = View.builder()
        .title('title')
        .type(View.Type.Search)
        .description('description')
        .search(Search.create().toBuilder().id('id-beef').build())
        .id('id-beef')
        .build();
      const viewStoreState = {
        activeQuery: '',
        view: view,
        dirty: true,
        isNew: false,
      };
      render(<SimpleSavedSearchControls viewStoreState={viewStoreState} />);

      await screen.findByRole('button', { name: 'Unsaved changes' });
    });
  });
});
