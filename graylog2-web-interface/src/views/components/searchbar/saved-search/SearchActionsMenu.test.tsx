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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { defaultUser } from 'defaultMockValues';

import { asMock, MockStore } from 'helpers/mocking';
import { adminUser } from 'fixtures/users';
import mockAction from 'helpers/mocking/MockAction';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import type { ViewLoaderContextType } from 'views/logic/ViewLoaderContext';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import type { NewViewLoaderContextType } from 'views/logic/NewViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import * as ViewsPermissions from 'views/Permissions';
import history from 'util/History';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { ViewStore } from 'views/stores/ViewStore';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';
import useCurrentUser from 'hooks/useCurrentUser';

import SearchActionsMenu from './SearchActionsMenu';

jest.mock('routing/Routes', () => ({ pluginRoute: (x) => x }));
jest.mock('views/hooks/useSaveViewFormControls');
jest.mock('util/History');
jest.mock('hooks/useCurrentUser');

jest.mock('bson-objectid', () => jest.fn(() => ({
  toString: jest.fn(() => 'new-search-id'),
})));

jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {
    create: mockAction(),
    load: mockAction(),
  },
  ViewStore: MockStore(),
}));

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    create: jest.fn((v) => Promise.resolve(v)).mockName('create'),
  },
}));

describe('SearchActionsMenu', () => {
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

  type SimpleSearchActionsMenuProps = {
    loadNewView?: NewViewLoaderContextType,
    onLoadView?: ViewLoaderContextType,
  };

  const SimpleSearchActionsMenu = ({
    loadNewView = () => Promise.resolve(),
    onLoadView,
    ...props
  }: SimpleSearchActionsMenuProps) => (
    <FieldTypesContext.Provider value={fieldTypes}>
      <ViewLoaderContext.Provider value={onLoadView}>
        <NewViewLoaderContext.Provider value={loadNewView}>
          <SearchActionsMenu {...props} />
        </NewViewLoaderContext.Provider>
      </ViewLoaderContext.Provider>
    </FieldTypesContext.Provider>
  );

  const findShareButton = () => screen.findByRole('button', { name: 'Share' });
  const expectShareButton = findShareButton;
  const findCreateNewButton = () => screen.findByRole('button', { name: /create new/i });

  SimpleSearchActionsMenu.defaultProps = {
    loadNewView: () => Promise.resolve(),
    onLoadView: () => Promise.resolve(),
  };

  beforeEach(() => {
    asMock(ViewStore.getInitialState).mockReturnValue(defaultViewStoreState);
    asMock(useSaveViewFormControls).mockReturnValue([]);
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  describe('Button handling', () => {
    const findTitleInput = () => screen.getByRole('textbox', { name: /title/i });

    it('should export current search as dashboard', async () => {
      asMock(useCurrentUser).mockReturnValue(
        adminUser.toBuilder()
          .permissions(Immutable.List(['dashboards:create']))
          .build(),
      );

      render(<SimpleSearchActionsMenu />);
      const exportAsDashboardMenuItem = await screen.findByText('Export to dashboard');
      userEvent.click(exportAsDashboardMenuItem);
      await waitFor(() => expect(history.push).toHaveBeenCalledTimes(1));

      expect(history.push).toHaveBeenCalledWith({
        pathname: 'DASHBOARDS_NEW',
        state: { view: defaultViewStoreState.view },
      });
    });

    it('should not allow exporting search as dashboard if user does not have required permissions', async () => {
      asMock(useCurrentUser).mockReturnValue(
        adminUser.toBuilder()
          .permissions(Immutable.List([]))
          .build(),
      );

      render(<SimpleSearchActionsMenu />);

      await screen.findByText('Export');

      expect(screen.queryByText('Export to dashboard')).not.toBeInTheDocument();
    });

    it('should open file export modal', async () => {
      render(<SimpleSearchActionsMenu />);

      const exportMenuItem = await screen.findByText('Export');
      userEvent.click(exportMenuItem);

      await screen.findByText('Export all search results');
    });

    it('should open search metadata modal', async () => {
      asMock(useCurrentUser).mockReturnValue(
        adminUser.toBuilder()
          .permissions(Immutable.List([ViewsPermissions.View.Edit('some-id')]))
          .build(),
      );

      asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false, 'some-id'));
      render(<SimpleSearchActionsMenu />);
      const exportMenuItem = await screen.findByText('Edit metadata');
      userEvent.click(exportMenuItem);

      await screen.findByText('Editing saved search');
    });

    it('should clear a view', async () => {
      const loadNewView = jest.fn(() => Promise.resolve());

      render(<SimpleSearchActionsMenu loadNewView={loadNewView} />);

      const resetSearch = await screen.findByText('Reset search');
      userEvent.click(resetSearch);

      expect(loadNewView).toHaveBeenCalledTimes(1);
    });

    it('should loadView after create', async () => {
      ViewManagementActions.create = mockAction(jest.fn((view) => Promise.resolve(view)));
      const onLoadView = jest.fn((view) => Promise.resolve(view));
      asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false));

      render(<SimpleSearchActionsMenu onLoadView={onLoadView} />);

      userEvent.click(await screen.findByTitle('Save search'));
      userEvent.type(await findTitleInput(), 'Test');
      userEvent.click(await findCreateNewButton());

      await waitFor(() => expect(onLoadView).toHaveBeenCalledTimes(1));
    });

    it('should duplicate a saved search', async () => {
      asMock(useCurrentUser).mockReturnValue(
        adminUser.toBuilder()
          .permissions(Immutable.List([]))
          .build(),
      );

      const viewStoreState = createViewStoreState(false, 'some-id');
      asMock(ViewStore.getInitialState).mockReturnValue(viewStoreState);
      render(<SimpleSearchActionsMenu />);

      userEvent.click(await screen.findByTitle('Saved search'));
      userEvent.type(await findTitleInput(), ' and further title');
      userEvent.click(await findCreateNewButton());

      const updatedView = viewStoreState.view.toBuilder()
        .title('title and further title')
        .id('new-search-id')
        .build();

      await waitFor(() => expect(ViewManagementActions.create).toHaveBeenCalledWith(updatedView));
    });

    it('should extend a saved search with plugin data on duplication', async () => {
      asMock(useCurrentUser).mockReturnValue(
        adminUser.toBuilder()
          .permissions(Immutable.List([]))
          .build(),
      );

      asMock(useSaveViewFormControls).mockReturnValue([{
        component: () => <div>Pluggable component!</div>,
        id: 'example-plugin-component',
        onSearchDuplication: (view: View) => Promise.resolve(view.toBuilder().summary('This search has been extended by a plugin').build()),
      }]);

      const viewStoreState = createViewStoreState(false, 'some-id');
      asMock(ViewStore.getInitialState).mockReturnValue(viewStoreState);
      render(<SimpleSearchActionsMenu />);

      userEvent.click(await screen.findByTitle('Saved search'));
      userEvent.type(await findTitleInput(), ' and further title');
      userEvent.click(await findCreateNewButton());

      const updatedView = viewStoreState.view.toBuilder()
        .title('title and further title')
        .summary('This search has been extended by a plugin')
        .id('new-search-id')
        .build();

      await waitFor(() => expect(ViewManagementActions.create).toHaveBeenCalledWith(updatedView));
    });

    describe('has "Share" option', () => {
      it('includes the option to share the current search', async () => {
        asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false, 'some-id'));
        render(<SimpleSearchActionsMenu />);

        await expectShareButton();
      });

      it('which should be disabled if current user is neither owner nor permitted to edit search', async () => {
        asMock(useCurrentUser).mockReturnValue(
          adminUser.toBuilder()
            .grnPermissions(Immutable.List())
            .permissions(Immutable.List())
            .build(),
        );

        asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false, 'some-id'));

        render(<SimpleSearchActionsMenu />);

        const shareButton = await findShareButton();

        expect(shareButton).toBeDisabled();
      });

      it('which should be enabled if current user is permitted to edit search', async () => {
        asMock(useCurrentUser).mockReturnValue(
          adminUser.toBuilder()
            .grnPermissions(Immutable.List([`entity:own:grn::::search:${adminUser.id}`]))
            .permissions(Immutable.List([ViewsPermissions.View.Edit(adminUser.id)]))
            .build(),
        );

        asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false, adminUser.id));

        render(<SimpleSearchActionsMenu />);

        const shareButton = await findShareButton();

        expect(shareButton).not.toBeDisabled();
      });

      it('which should be enabled if current user is owner of search', async () => {
        asMock(useCurrentUser).mockReturnValue(
          adminUser.toBuilder()
            .grnPermissions(Immutable.List([`entity:own:grn::::search:${adminUser.id}`]))
            .permissions(Immutable.List())
            .build(),
        );

        asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false, adminUser.id));

        render(<SimpleSearchActionsMenu />);

        const shareButton = await findShareButton();

        expect(shareButton).not.toBeDisabled();
      });

      it('which should be enabled if current user is admin', async () => {
        asMock(useCurrentUser).mockReturnValue(adminUser);
        asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false, adminUser.id));

        render(<SimpleSearchActionsMenu />);

        const shareSearch = await findShareButton();

        expect(shareSearch).not.toBeDisabled();
      });

      it('which should be disabled if search is unsaved', async () => {
        render(<SimpleSearchActionsMenu />);

        expect(await findShareButton()).toBeDisabled();
      });
    });
  });

  describe('render the SearchActionsMenu', () => {
    it('should render not dirty with unsaved view', async () => {
      asMock(ViewStore.getInitialState).mockReturnValue(createViewStoreState(false));
      render(<SimpleSearchActionsMenu />);

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
      render(<SimpleSearchActionsMenu />);

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
      render(<SimpleSearchActionsMenu />);

      await screen.findByRole('button', { name: 'Unsaved changes' });
    });
  });
});
