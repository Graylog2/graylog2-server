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
import { SavedSearchesActions } from 'views/stores/SavedSearchesStore';

import SavedSearchList from './SavedSearchList';

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

  return Promise.resolve({
    pagination: {
      total: count,
      page: count > 0 ? count : 0,
      perPage: 5,
      count,
    },
    list: views,
  });
};

jest.mock('views/stores/SavedSearchesStore', () => ({
  SavedSearchesStore: {
    listen: jest.fn(),
  },
  SavedSearchesActions: {
    search: jest.fn(),
  },
}));

describe('SavedSearchList', () => {
  describe('render the SavedSearchList', () => {
    it('should render empty', async () => {
      const views = createPaginatedSearches(0);
      asMock(SavedSearchesActions.search).mockReturnValueOnce(views);

      render(<SavedSearchList toggleModal={() => {}}
                              deleteSavedSearch={() => Promise.resolve(views[0])}
                              activeSavedSearchId="search-id-0" />);

      await screen.findByText('No saved searches found.');
    });

    it('should render with views', async () => {
      const views = createPaginatedSearches(2);
      asMock(SavedSearchesActions.search).mockReturnValueOnce(views);

      render(<SavedSearchList toggleModal={() => {}}
                              deleteSavedSearch={() => Promise.resolve(views[0])}
                              activeSavedSearchId="search-id-0" />);

      await screen.findByText('search-title-0');
      await screen.findByText('search-title-1');
    });

    it('should handle toggle modal', async () => {
      const onToggleModal = jest.fn();
      const views = createPaginatedSearches(1);
      asMock(SavedSearchesActions.search).mockReturnValueOnce(views);
      const { getByText } = render(<SavedSearchList toggleModal={onToggleModal}
                                                    deleteSavedSearch={() => Promise.resolve(views[0])}
                                                    activeSavedSearchId="search-id-0" />);

      await screen.findByText('search-title-0');

      const cancel = getByText('Cancel');

      fireEvent.click(cancel);

      expect(onToggleModal).toBeCalledTimes(1);
    });

    it('should call `onDelete` if saved search is deleted', async () => {
      window.confirm = jest.fn(() => true);
      const views = createPaginatedSearches(1);
      asMock(SavedSearchesActions.search).mockReturnValueOnce(views);
      const onDelete = jest.fn(() => Promise.resolve(views[0]));

      render(<SavedSearchList toggleModal={() => {}}
                              deleteSavedSearch={onDelete}
                              activeSavedSearchId="search-id-0" />);

      await screen.findByText('search-title-0');
      const deleteBtn = screen.getByTitle('Delete search search-title-0');

      fireEvent.click(deleteBtn);

      expect(window.confirm).toBeCalledTimes(1);

      await waitFor(() => expect(onDelete).toBeCalledTimes(1));
    });

    it('should call load function from context', async () => {
      const onLoad = jest.fn(() => new Promise(() => {}));
      const views = createPaginatedSearches(1);
      asMock(SavedSearchesActions.search).mockReturnValueOnce(views);

      render(
        <ViewLoaderContext.Provider value={onLoad}>
          <SavedSearchList toggleModal={() => {}}
                           deleteSavedSearch={() => Promise.resolve(views[0])}
                           activeSavedSearchId="search-id-0" />
        </ViewLoaderContext.Provider>,
      );

      const listItem = await screen.findByText('search-title-0');

      fireEvent.click(listItem);

      expect(onLoad).toBeCalledTimes(1);
    });
  });
});
