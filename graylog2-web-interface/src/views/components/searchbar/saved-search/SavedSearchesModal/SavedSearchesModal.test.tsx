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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import Immutable from 'immutable';

import asMock from 'helpers/mocking/AsMock';
import View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import useSavedSearches from 'views/hooks/useSavedSearches';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import { adminUser } from 'fixtures/users';
import useCurrentUser from 'hooks/useCurrentUser';

import SavedSearchesModal from './SavedSearchesModal';

const createPaginatedSearches = (count = 1) => {
  const views: Array<View> = [];

  if (count > 0) {
    // eslint-disable-next-line no-plusplus
    for (let i = 0; i < count; i++) {
      views.push(
        View.builder()
          .id(`search-id-${i}`)
          .title(`search-title-${i}`)
          .description('desc')
          .build(),
      );
    }
  }

  return ({
    pagination: {
      total: count,
      page: count > 0 ? count : 0,
      perPage: 5,
      count,
    },
    attributes: [
      {
        id: 'title',
        title: 'Title',
        sortable: true,
      },
      {
        id: 'description',
        title: 'Description',
        sortable: true,
      },
    ],
    list: views,
  });
};

jest.mock('views/hooks/useSavedSearches');
jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');
jest.mock('components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences');
jest.mock('hooks/useCurrentUser');

jest.mock('routing/Routes', () => ({
  getPluginRoute: (x) => () => x,
}));

describe('SavedSearchesModal', () => {
  const defaultPaginatedSearches = createPaginatedSearches();

  beforeEach(() => {
    asMock(useSavedSearches).mockReturnValue({
      data: defaultPaginatedSearches,
      refetch: () => {},
      isInitialLoading: false,
    });

    asMock(useUserLayoutPreferences).mockReturnValue({ data: layoutPreferences, isInitialLoading: false });
    asMock(useUpdateUserLayoutPreferences).mockReturnValue({ mutate: () => {} });
    asMock(useCurrentUser).mockReturnValue(adminUser);
  });

  describe('render the SavedSearchesModal', () => {
    it('should render empty', async () => {
      const paginatedSavedSearches = createPaginatedSearches(0);

      asMock(useSavedSearches).mockReturnValue({
        data: paginatedSavedSearches,
        refetch: () => {},
        isInitialLoading: false,
      });

      render(<SavedSearchesModal toggleModal={() => {}}
                                 deleteSavedSearch={() => Promise.resolve()}
                                 activeSavedSearchId="search-id-0" />);

      await screen.findByText('No saved searches have been created yet.');
    });

    it('should render with views', async () => {
      const paginatedSavedSearches = createPaginatedSearches(2);

      asMock(useSavedSearches).mockReturnValue({
        data: paginatedSavedSearches,
        refetch: () => {},
        isInitialLoading: false,
      });

      render(<SavedSearchesModal toggleModal={() => {}}
                                 deleteSavedSearch={() => Promise.resolve()}
                                 activeSavedSearchId="search-id-0" />);

      await screen.findByText('search-title-0');
      await screen.findByText('search-title-1');
    });

    it('should handle toggle modal', async () => {
      const onToggleModal = jest.fn();
      const { getByText } = render(<SavedSearchesModal toggleModal={onToggleModal}
                                                       deleteSavedSearch={() => Promise.resolve()}
                                                       activeSavedSearchId="search-id-0" />);

      await screen.findByText('search-title-0');

      const cancel = getByText('Cancel');

      userEvent.click(cancel);

      expect(onToggleModal).toHaveBeenCalledTimes(1);
    });

    it('should call `onDelete` if saved search is deleted', async () => {
      window.confirm = jest.fn(() => true);
      const onDelete = jest.fn(() => Promise.resolve());

      render(<SavedSearchesModal toggleModal={() => {}}
                                 deleteSavedSearch={onDelete}
                                 activeSavedSearchId="search-id-0" />);

      await screen.findByText('search-title-0');
      const deleteBtn = screen.getByTitle('Delete search search-title-0');

      userEvent.click(deleteBtn);

      expect(window.confirm).toHaveBeenCalledTimes(1);

      await waitFor(() => expect(onDelete).toHaveBeenCalledTimes(1));
    });

    it('should call load function from context', async () => {
      const onLoad = jest.fn(() => new Promise(() => {}));

      render(
        <ViewLoaderContext.Provider value={onLoad}>
          <SavedSearchesModal toggleModal={() => {}}
                              deleteSavedSearch={() => Promise.resolve()}
                              activeSavedSearchId="search-id-0" />
        </ViewLoaderContext.Provider>,
      );

      const listItem = await screen.findByText('search-title-0');

      userEvent.click(listItem);

      expect(onLoad).toHaveBeenCalledTimes(1);
    });

    it('should not display delete action for saved search when user is missing required permissions', async () => {
      const currentUser = adminUser.toBuilder().permissions(Immutable.List([`view:read:${defaultPaginatedSearches.list[0].id}`])).build();
      asMock(useCurrentUser).mockReturnValue(currentUser);

      render(<SavedSearchesModal toggleModal={() => {}}
                                 deleteSavedSearch={jest.fn()}
                                 activeSavedSearchId="search-id-0" />);

      await screen.findByText('search-title-0');

      expect(screen.queryByTitle('Delete search search-title-0')).not.toBeInTheDocument();
    });

    it('should update layout setting when changing page size', async () => {
      const updateTableLayout = jest.fn();

      asMock(useUpdateUserLayoutPreferences).mockReturnValue({
        mutate: updateTableLayout,
      });

      render(<SavedSearchesModal toggleModal={() => {}}
                                 deleteSavedSearch={() => Promise.resolve()}
                                 activeSavedSearchId="search-id-0" />);

      const pageSizeDropdown = await screen.findByRole('button', {
        name: /configure page size/i,
        hidden: true,
      });

      userEvent.click(pageSizeDropdown);

      const pageSizeOption = await screen.findByRole('menuitem', {
        name: /100/i,
        hidden: true,
      });

      userEvent.click(pageSizeOption);

      expect(updateTableLayout).toHaveBeenCalledTimes(1);
      expect(updateTableLayout).toHaveBeenCalledWith({ perPage: 100 });
    });
  });
});
