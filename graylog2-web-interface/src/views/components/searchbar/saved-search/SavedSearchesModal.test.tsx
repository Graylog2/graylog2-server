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
import { render, fireEvent, screen, waitFor } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import useSavedSearches from 'views/hooks/useSavedSearches';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';

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

jest.mock('routing/Routes', () => ({
  getPluginRoute: (x) => () => x,
}));

describe('SavedSearchesModal', () => {
  const defaultPaginatedSearches = createPaginatedSearches();

  beforeEach(() => {
    asMock(useSavedSearches).mockReturnValue({
      data: defaultPaginatedSearches,
      refetch: () => {},
      isLoading: false,
    });

    asMock(useUserLayoutPreferences).mockReturnValue({ data: layoutPreferences, isLoading: false });
  });

  describe('render the SavedSearchesModal', () => {
    it('should render empty', async () => {
      const paginatedSavedSearches = createPaginatedSearches(0);

      asMock(useSavedSearches).mockReturnValue({
        data: paginatedSavedSearches,
        refetch: () => {},
        isLoading: false,
      });

      render(<SavedSearchesModal toggleModal={() => {}}
                                 deleteSavedSearch={() => Promise.resolve(paginatedSavedSearches.list[0])}
                                 activeSavedSearchId="search-id-0" />);

      await screen.findByText('No saved searches have been created yet.');
    });

    it('should render with views', async () => {
      const paginatedSavedSearches = createPaginatedSearches(2);

      asMock(useSavedSearches).mockReturnValue({
        data: paginatedSavedSearches,
        refetch: () => {},
        isLoading: false,
      });

      render(<SavedSearchesModal toggleModal={() => {}}
                                 deleteSavedSearch={() => Promise.resolve(paginatedSavedSearches.list[0])}
                                 activeSavedSearchId="search-id-0" />);

      await screen.findByText('search-title-0');
      await screen.findByText('search-title-1');
    });

    it('should handle toggle modal', async () => {
      const onToggleModal = jest.fn();
      const { getByText } = render(<SavedSearchesModal toggleModal={onToggleModal}
                                                       deleteSavedSearch={() => Promise.resolve(defaultPaginatedSearches.list[0])}
                                                       activeSavedSearchId="search-id-0" />);

      await screen.findByText('search-title-0');

      const cancel = getByText('Cancel');

      fireEvent.click(cancel);

      expect(onToggleModal).toHaveBeenCalledTimes(1);
    });

    it('should call `onDelete` if saved search is deleted', async () => {
      window.confirm = jest.fn(() => true);
      const onDelete = jest.fn(() => Promise.resolve(defaultPaginatedSearches.list[0]));

      render(<SavedSearchesModal toggleModal={() => {}}
                                 deleteSavedSearch={onDelete}
                                 activeSavedSearchId="search-id-0" />);

      await screen.findByText('search-title-0');
      const deleteBtn = screen.getByTitle('Delete search search-title-0');

      fireEvent.click(deleteBtn);

      expect(window.confirm).toHaveBeenCalledTimes(1);

      await waitFor(() => expect(onDelete).toHaveBeenCalledTimes(1));
    });

    it('should call load function from context', async () => {
      const onLoad = jest.fn(() => new Promise(() => {}));

      render(
        <ViewLoaderContext.Provider value={onLoad}>
          <SavedSearchesModal toggleModal={() => {}}
                              deleteSavedSearch={() => Promise.resolve(defaultPaginatedSearches.list[0])}
                              activeSavedSearchId="search-id-0" />
        </ViewLoaderContext.Provider>,
      );

      const listItem = await screen.findByText('search-title-0');

      fireEvent.click(listItem);

      expect(onLoad).toHaveBeenCalledTimes(1);
    });
  });
});
