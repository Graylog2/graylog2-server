// @flow strict
import React from 'react';
import { render, cleanup, fireEvent, wait } from 'wrappedTestingLibrary';
import { browserHistory } from 'react-router';
import Routes from 'routing/Routes';

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
    afterEach(() => {
      cleanup();
    });
    it('should render empty', () => {
      const views = createViewsResponse(0);
      const { baseElement } = render(<SavedSearchList toggleModal={() => {}}
                                                      showModal
                                                      deleteSavedSearch={() => {}}
                                                      views={views} />);
      expect(baseElement).toMatchSnapshot();
    });

    it('should render with views', () => {
      const views = createViewsResponse(1);
      const { baseElement } = render(<SavedSearchList toggleModal={() => {}}
                                                      showModal
                                                      deleteSavedSearch={() => {}}
                                                      views={views} />);
      expect(baseElement).toMatchSnapshot();
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

    it('should handle with delete', () => {
      window.confirm = jest.fn(() => true);
      const onDelete = jest.fn(() => {
        return new Promise(() => {});
      });
      const views = createViewsResponse(1);
      const { getByText } = render(<SavedSearchList toggleModal={() => {}}
                                                    showModal
                                                    deleteSavedSearch={onDelete}
                                                    views={views} />);
      const listItem = getByText('test-0');
      fireEvent.click(listItem);
      const deleteBtn = getByText('Delete');
      fireEvent.click(deleteBtn);
      expect(window.confirm).toBeCalledTimes(1);
      expect(onDelete).toBeCalledTimes(1);
    });

    it('should handle with load', () => {
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
      const loadBtn = getByText('Load');
      fireEvent.click(loadBtn);
      expect(onLoad).toBeCalledTimes(1);
    });
  });
  describe('load new saved search', () => {
    afterEach(() => {
      cleanup();
    });
    it('should change url after load', async () => {
      const onLoad = jest.fn(() => Promise.resolve());
      Routes.pluginRoute = jest.fn(route => id => `${route}:${id}`);
      browserHistory.push = jest.fn();
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
      const loadButton = getByText('Load');
      fireEvent.click(loadButton);
      await wait(() => {
        expect(browserHistory.push).toBeCalledTimes(1);
        expect(browserHistory.push).toHaveBeenCalledWith('SEARCH_VIEWID:foo-bar-0');
      });
    });
  });
});
