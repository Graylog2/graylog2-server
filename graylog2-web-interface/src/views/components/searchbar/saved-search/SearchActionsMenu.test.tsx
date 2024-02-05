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
import { render, screen, waitFor, waitForElementToBeRemoved } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { defaultUser } from 'defaultMockValues';

import { asMock } from 'helpers/mocking';
import { adminUser } from 'fixtures/users';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import type { ViewLoaderContextType } from 'views/logic/ViewLoaderContext';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import type { NewViewLoaderContextType } from 'views/logic/NewViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import * as ViewsPermissions from 'views/Permissions';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';
import useCurrentUser from 'hooks/useCurrentUser';
import useView from 'views/hooks/useView';
import useIsDirty from 'views/hooks/useIsDirty';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import useIsNew from 'views/hooks/useIsNew';
import useHistory from 'routing/useHistory';
import mockHistory from 'helpers/mocking/mockHistory';
import OnSaveViewAction from 'views/logic/views/OnSaveViewAction';
import HotkeysProvider from 'contexts/HotkeysProvider';

import SearchActionsMenu from './SearchActionsMenu';

jest.mock('views/hooks/useSaveViewFormControls');
jest.mock('routing/useHistory');
jest.mock('hooks/useCurrentUser');
jest.mock('views/logic/views/OnSaveViewAction', () => jest.fn(() => () => {}));
jest.mock('hooks/useFeature', () => (featureFlag: string) => featureFlag === 'frontend_hotkeys');

jest.mock('bson-objectid', () => jest.fn(() => ({
  toString: jest.fn(() => 'new-search-id'),
})));

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    create: jest.fn((v) => Promise.resolve(v)).mockName('create'),
  },
}));

jest.mock('views/hooks/useView');
jest.mock('views/hooks/useIsDirty');
jest.mock('views/hooks/useIsNew');

jest.mock('views/logic/slices/viewSlice', () => {
  const originalModule = jest.requireActual('views/logic/slices/viewSlice');

  return {
    ...originalModule,
    loadView: jest.fn(() => () => {}),
  };
});

describe('SearchActionsMenu', () => {
  const createView = (id: string = undefined) => View.builder()
    .id(id)
    .title('title')
    .type(View.Type.Search)
    .description('description')
    .state(Immutable.Map())
    .owner('owningUser')
    .build();

  const defaultView = createView();

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
    <TestStoreProvider>
      <HotkeysProvider>
        <FieldTypesContext.Provider value={fieldTypes}>
          <ViewLoaderContext.Provider value={onLoadView}>
            <NewViewLoaderContext.Provider value={loadNewView}>
              <SearchActionsMenu {...props} />
            </NewViewLoaderContext.Provider>
          </ViewLoaderContext.Provider>
        </FieldTypesContext.Provider>
      </HotkeysProvider>
    </TestStoreProvider>
  );

  const findShareButton = () => screen.findByRole('button', { name: 'Share' });
  const expectShareButton = findShareButton;
  const findCreateNewButton = () => screen.findByRole('button', { name: /create new/i });

  SimpleSearchActionsMenu.defaultProps = {
    loadNewView: () => Promise.resolve(),
    onLoadView: () => Promise.resolve(),
  };

  beforeEach(() => {
    asMock(useSaveViewFormControls).mockReturnValue([]);
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(useView).mockReturnValue(defaultView);
    asMock(useIsDirty).mockReturnValue(false);
    asMock(useIsNew).mockReturnValue(false);
  });

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  describe('Button handling', () => {
    let history;

    beforeEach(() => {
      history = mockHistory();
      asMock(useHistory).mockReturnValue(history);
    });

    const findTitleInput = () => screen.findByRole('textbox', { name: /title/i });

    it('should export current search as dashboard', async () => {
      asMock(useCurrentUser).mockReturnValue(
        adminUser.toBuilder()
          .permissions(Immutable.List(['dashboards:create']))
          .build(),
      );

      render(<SimpleSearchActionsMenu />);
      userEvent.click(await screen.findByRole('button', { name: /open search actions/i }));
      const exportAsDashboardMenuItem = await screen.findByText('Export to dashboard');
      userEvent.click(exportAsDashboardMenuItem);
      await waitFor(() => expect(history.pushWithState).toHaveBeenCalledTimes(1));

      expect(history.pushWithState).toHaveBeenCalledWith('/dashboards/new', { view: defaultView });
    });

    it('should not allow exporting search as dashboard if user does not have required permissions', async () => {
      asMock(useCurrentUser).mockReturnValue(
        adminUser.toBuilder()
          .permissions(Immutable.List([]))
          .build(),
      );

      render(<SimpleSearchActionsMenu />);
      userEvent.click(await screen.findByRole('button', { name: /open search actions/i }));

      await screen.findByText('Export');

      expect(screen.queryByText('Export to dashboard')).not.toBeInTheDocument();
    });

    it('should open file export modal', async () => {
      render(<SimpleSearchActionsMenu />);
      userEvent.click(await screen.findByRole('button', { name: /open search actions/i }));

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

      asMock(useView).mockReturnValue(createView('some-id'));
      render(<SimpleSearchActionsMenu />);
      userEvent.click(await screen.findByRole('button', { name: /open search actions/i }));
      const exportMenuItem = await screen.findByText('Edit metadata');
      userEvent.click(exportMenuItem);

      await screen.findByText('Editing saved search');
    });

    it('should clear a view', async () => {
      asMock(useIsDirty).mockReturnValue(true);
      const loadNewView = jest.fn(() => Promise.resolve());

      render(<SimpleSearchActionsMenu loadNewView={loadNewView} />);
      userEvent.click(await screen.findByRole('button', { name: /open search actions/i }));

      const resetSearch = await screen.findByText('Reset search');

      userEvent.click(resetSearch);

      await waitFor(() => { expect(loadNewView).toHaveBeenCalledTimes(1); });
    });

    it('should loadView after create', async () => {
      asMock(useIsNew).mockReturnValue(true);
      const onLoadView = jest.fn((view) => Promise.resolve(view));

      render(<SimpleSearchActionsMenu onLoadView={onLoadView} />);

      userEvent.click(await screen.findByTitle('Save search'));
      userEvent.type(await findTitleInput(), 'Test');
      userEvent.click(await findCreateNewButton());

      await waitFor(() => expect(onLoadView).toHaveBeenCalledTimes(1));
    });

    it('should duplicate a saved search', async () => {
      asMock(useView).mockReturnValue(defaultView.toBuilder().id('some-id-1').title('title').build());

      asMock(useCurrentUser).mockReturnValue(
        adminUser.toBuilder()
          .permissions(Immutable.List([]))
          .build(),
      );

      render(<SimpleSearchActionsMenu />);

      userEvent.click(await screen.findByTitle('Saved search'));
      userEvent.type(await findTitleInput(), ' and further title');
      userEvent.click(await findCreateNewButton());

      const updatedView = defaultView.toBuilder()
        .title('title and further title')
        .id('new-search-id')
        .build();

      await waitFor(() => expect(ViewManagementActions.create).toHaveBeenCalledWith(updatedView));
    });

    it('should extend a saved search with plugin data on duplication', async () => {
      asMock(useView).mockReturnValue(defaultView.toBuilder().id('some-id-1').build());

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

      render(<SimpleSearchActionsMenu />);

      userEvent.click(await screen.findByTitle('Saved search'));
      userEvent.type(await findTitleInput(), ' and further title');
      userEvent.click(await findCreateNewButton());

      const updatedView = defaultView.toBuilder()
        .title('title and further title')
        .summary('This search has been extended by a plugin')
        .id('new-search-id')
        .build();

      await waitFor(() => expect(ViewManagementActions.create).toHaveBeenCalledWith(updatedView));
      await waitForElementToBeRemoved(screen.queryByText('Pluggable component!'));
    });

    it('should save search when pressing related keyboard shortcut', async () => {
      asMock(useView).mockReturnValue(createView('some-id'));
      render(<SimpleSearchActionsMenu />);
      userEvent.keyboard('{Meta>}s{/Meta}');

      await waitFor(() => expect(OnSaveViewAction).toHaveBeenCalledTimes(1));
    });

    describe('has "Share" option', () => {
      beforeEach(() => {
        asMock(useView).mockReturnValue(defaultView.toBuilder().id(adminUser.id).build());
      });

      it('includes the option to share the current search', async () => {
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

        render(<SimpleSearchActionsMenu />);

        const shareButton = await findShareButton();

        expect(shareButton).not.toBeDisabled();
      });

      it('which should be enabled if current user is admin', async () => {
        asMock(useCurrentUser).mockReturnValue(adminUser);

        render(<SimpleSearchActionsMenu />);

        const shareSearch = await findShareButton();

        expect(shareSearch).not.toBeDisabled();
      });

      it('which should be disabled if search is unsaved', async () => {
        asMock(useIsNew).mockReturnValue(true);
        asMock(useView).mockReturnValue(defaultView);

        render(<SimpleSearchActionsMenu />);

        expect(await findShareButton()).toBeDisabled();
      });
    });
  });

  describe('render the SearchActionsMenu', () => {
    it('should render not dirty with unsaved view', async () => {
      asMock(useIsNew).mockReturnValue(true);
      asMock(useView).mockReturnValue(defaultView.toBuilder().id(undefined).build());

      render(<SimpleSearchActionsMenu />);

      await screen.findByRole('button', { name: 'Save search' });
    });

    it('should render not dirty', async () => {
      asMock(useView).mockReturnValue(View.builder()
        .title('title')
        .description('description')
        .type(View.Type.Search)
        .search(Search.create().toBuilder().id('id-beef').build())
        .id('id-beef')
        .build());

      asMock(useIsDirty).mockReturnValue(false);

      render(<SimpleSearchActionsMenu />);

      await screen.findByRole('button', { name: 'Saved search' });
    });

    it('should render dirty', async () => {
      asMock(useView).mockReturnValue(View.builder()
        .title('title')
        .type(View.Type.Search)
        .description('description')
        .search(Search.create().toBuilder().id('id-beef').build())
        .id('id-beef')
        .build());

      asMock(useIsDirty).mockReturnValue(true);

      render(<SimpleSearchActionsMenu />);

      await screen.findByRole('button', { name: 'Unsaved changes' });
    });
  });
});
