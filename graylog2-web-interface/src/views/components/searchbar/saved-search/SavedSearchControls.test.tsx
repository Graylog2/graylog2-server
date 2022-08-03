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
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { asMock, MockStore } from 'helpers/mocking';
import { adminUser, alice as currentUser } from 'fixtures/users';
import mockAction from 'helpers/mocking/MockAction';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import type { ViewLoaderContextType } from 'views/logic/ViewLoaderContext';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import type { NewViewLoaderContextType } from 'views/logic/NewViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import * as ViewsPermissions from 'views/Permissions';
import CurrentUserContext from 'contexts/CurrentUserContext';
import type User from 'logic/users/User';
import history from 'util/History';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { ViewStore } from 'views/stores/ViewStore';

import SavedSearchControls from './SavedSearchControls';

jest.mock('routing/Routes', () => ({ pluginRoute: (x) => x }));

jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {
    create: { completed: { listen: jest.fn() } },
    load: { completed: { listen: jest.fn() } },
  },
  ViewStore: MockStore(),
}));

jest.mock('util/History');

describe('SavedSearchControls', () => {
  const createViewStoreState = (dirty = true, id = undefined) => ({
    activeQuery: '',
    view: View.builder()
      .id(id)
      .title('title')
      .type(View.Type.Search)
      .description('description')
      .state(Immutable.Map())
      .search(Search.create().toBuilder().id('id-beef').build())
      .owner('owningUser')
      .build(),
    dirty,
    isNew: false,
  });

  const defaultViewStoreState = createViewStoreState();

  const fieldTypes = {
    all: Immutable.List<FieldTypeMapping>(),
    queryFields: Immutable.Map<string, Immutable.List<FieldTypeMapping>>(),
  };

  type SimpleSavedSearchControlsProps = {
    loadNewView?: NewViewLoaderContextType,
    onLoadView?: ViewLoaderContextType,
    currentUser?: User,
  };

  const SimpleSavedSearchControls = ({
    loadNewView = () => Promise.resolve(),
    onLoadView,
    currentUser: user,
    ...props
  }: SimpleSavedSearchControlsProps) => (
    <FieldTypesContext.Provider value={fieldTypes}>
      <ViewLoaderContext.Provider value={onLoadView}>
        <CurrentUserContext.Provider value={user}>
          <NewViewLoaderContext.Provider value={loadNewView}>
            <SavedSearchControls {...props} />
          </NewViewLoaderContext.Provider>
        </CurrentUserContext.Provider>
      </ViewLoaderContext.Provider>
    </FieldTypesContext.Provider>
  );

  const findShareButton = () => screen.findByRole('button', { name: 'Share' });
  const expectShareButton = findShareButton;

  SimpleSavedSearchControls.defaultProps = {
    loadNewView: () => Promise.resolve(),
    onLoadView: () => Promise.resolve(),
    currentUser,
  };

  describe('Button handling', () => {
    beforeEach(() => {
      asMock(ViewStore.getInitialState).mockReturnValue(defaultViewStoreState);
    });

    it('should export current search as dashboard', async () => {
      const user = currentUser.toBuilder()
        .permissions(Immutable.List(['dashboards:create']))
        .build();
      render(<SimpleSavedSearchControls currentUser={user} />);

      const exportAsDashboardMenuItem = await screen.findByText('Export to dashboard');
      userEvent.click(exportAsDashboardMenuItem);
      await waitFor(() => expect(history.push).toHaveBeenCalledTimes(1));

      expect(history.push).toHaveBeenCalledWith({
        pathname: 'DASHBOARDS_NEW',
        state: { view: defaultViewStoreState.view },
      });
    });

    it('should not allow exporting search as dashboard if user does not have required permissions', async () => {
      const user = currentUser.toBuilder()
        .permissions(Immutable.List([]))
        .build();
      render(<SimpleSavedSearchControls currentUser={user} />);

      await screen.findByText('Export');

      expect(screen.queryByText('Export to dashboard')).not.toBeInTheDocument();
    });

    it('should open file export modal', async () => {
      render(<SimpleSavedSearchControls />);

      const exportMenuItem = await screen.findByText('Export');
      userEvent.click(exportMenuItem);

      await screen.findByText('Export all search results');
    });

    it('should open search metadata modal', async () => {
      const user = currentUser.toBuilder()
        .permissions(Immutable.List([ViewsPermissions.View.Edit('some-id')]))
        .build();
      asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false, 'some-id'));
      render(<SimpleSavedSearchControls currentUser={user} />);

      const exportMenuItem = await screen.findByText('Edit metadata');
      userEvent.click(exportMenuItem);

      await screen.findByText('Editing saved search');
    });

    it('should clear a view', async () => {
      const loadNewView = jest.fn(() => Promise.resolve());

      render(<SimpleSavedSearchControls loadNewView={loadNewView} />);

      const resetSearch = await screen.findByText('Reset search');
      fireEvent.click(resetSearch);

      expect(loadNewView).toHaveBeenCalledTimes(1);
    });

    it('should loadView after create', async () => {
      ViewManagementActions.create = mockAction(jest.fn((view) => Promise.resolve(view)));
      const onLoadView = jest.fn((view) => Promise.resolve(view));
      asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false));

      render(<SimpleSavedSearchControls onLoadView={onLoadView} />);

      fireEvent.click(await screen.findByTitle('Save search'));
      userEvent.type(await screen.findByPlaceholderText('Enter title'), 'Test');
      fireEvent.click(await screen.findByText('Create new'));

      await waitFor(() => expect(onLoadView).toHaveBeenCalledTimes(1));
    });

    describe('has "Share" option', () => {
      it('includes the option to share the current search', async () => {
        asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false, 'some-id'));
        render(<SimpleSavedSearchControls />);

        await expectShareButton();
      });

      it('which should be disabled if current user is neither owner nor permitted to edit search', async () => {
        const notOwningUser = currentUser.toBuilder()
          .grnPermissions(Immutable.List())
          .permissions(Immutable.List())
          .build();
        asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false, 'some-id'));

        render(<SimpleSavedSearchControls currentUser={notOwningUser} />);

        const shareButton = await findShareButton();

        expect(shareButton).toBeDisabled();
      });

      it('which should be enabled if current user is owner of search', async () => {
        const owningUser = currentUser.toBuilder()
          .grnPermissions(Immutable.List([`entity:own:grn::::search:${currentUser.id}`]))
          .permissions(Immutable.List())
          .build();
        asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false, owningUser.id));

        render(<SimpleSavedSearchControls currentUser={owningUser} />);

        const shareButton = await findShareButton();

        expect(shareButton).not.toBeDisabled();
      });

      it('which should be enabled if current user is permitted to edit search', async () => {
        const owningUser = currentUser.toBuilder()
          .grnPermissions(Immutable.List([`entity:own:grn::::search:${currentUser.id}`]))
          .permissions(Immutable.List([ViewsPermissions.View.Edit(currentUser.id)]))
          .build();
        asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false, owningUser.id));

        render(<SimpleSavedSearchControls currentUser={owningUser} />);

        const shareButton = await findShareButton();

        expect(shareButton).not.toBeDisabled();
      });

      it('which should be enabled if current user is admin', async () => {
        asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false, adminUser.id));

        render(<SimpleSavedSearchControls currentUser={adminUser} />);

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
      asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false));
      render(<SimpleSavedSearchControls />);

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
      asMock(ViewStore.getInitialState).mockReturnValue(viewStoreState);
      render(<SimpleSavedSearchControls />);

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
      asMock(ViewStore.getInitialState).mockReturnValue(viewStoreState);
      render(<SimpleSavedSearchControls />);

      await screen.findByRole('button', { name: 'Unsaved changes' });
    });
  });
});
