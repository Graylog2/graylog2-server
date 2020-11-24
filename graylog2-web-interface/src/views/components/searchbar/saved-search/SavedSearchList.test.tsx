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
import { render, fireEvent } from 'wrappedTestingLibrary';

import View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';

import SavedSearchList from './SavedSearchList';

const createViewsResponse = (count = 1) => {
  const views = [];

  if (count > 0) {
    // eslint-disable-next-line no-plusplus
    for (let i = 0; i < count; i++) {
      views.push(
        View.builder()
          .id(`foo-bar-${i}`)
          .title(`test-${i}`)
          .description('desc')
          .build(),
      );
    }
  }

  return {
    pagination: {
      total: count,
      page: count > 0 ? count : 0,
      perPage: 5,
    },
    list: views,
  };
};

describe('SavedSearchList', () => {
  describe('render the SavedSearchList', () => {
    it('should render empty', () => {
      const views = createViewsResponse(0);
      const { baseElement } = render(<SavedSearchList toggleModal={() => {}}
                                                      showModal
                                                      deleteSavedSearch={() => {}}
                                                      views={views} />);

      expect(baseElement).not.toBeNull();
    });

    it('should render with views', () => {
      const views = createViewsResponse(1);
      const { baseElement } = render(<SavedSearchList toggleModal={() => {}}
                                                      showModal
                                                      deleteSavedSearch={() => {}}
                                                      views={views} />);

      expect(baseElement).not.toBeNull();
    });

    it('should handle toggle modal', () => {
      const onToggleModal = jest.fn();
      const views = createViewsResponse(1);

      const { getByText } = render(<SavedSearchList toggleModal={onToggleModal}
                                                    showModal
                                                    deleteSavedSearch={() => {}}
                                                    views={views} />);

      const cancel = getByText('Cancel');

      fireEvent.click(cancel);

      expect(onToggleModal).toBeCalledTimes(1);
    });

    it('should call `onDelete` if saved search is deleted', () => {
      window.confirm = jest.fn(() => true);
      const onDelete = jest.fn(() => {
        return new Promise(() => {});
      });
      const views = createViewsResponse(1);
      const { getByTestId } = render(<SavedSearchList toggleModal={() => {}}
                                                      showModal
                                                      deleteSavedSearch={onDelete}
                                                      views={views} />);
      const deleteBtn = getByTestId('delete-foo-bar-0');

      fireEvent.click(deleteBtn);

      expect(window.confirm).toBeCalledTimes(1);
      expect(onDelete).toBeCalledTimes(1);
    });

    it('should call load function from context', () => {
      const onLoad = jest.fn(() => { return new Promise(() => {}); });
      const views = createViewsResponse(1);

      const { getByText } = render(
        <ViewLoaderContext.Provider value={onLoad}>
          <SavedSearchList toggleModal={() => {}}
                           showModal
                           deleteSavedSearch={() => {}}
                           views={views} />
        </ViewLoaderContext.Provider>,
      );
      const listItem = getByText('test-0');

      fireEvent.click(listItem);

      expect(onLoad).toBeCalledTimes(1);
    });
  });
});
