// @flow strict
import React from 'react';
import { mount } from 'enzyme';

import View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import BookmarkList from './BookmarkList';

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

describe('BookmarkList', () => {
  describe('render the BookmarkList', () => {
    it('should render empty', () => {
      const views = createViewsResponse(0);
      const wrapper = mount(<BookmarkList toggleModal={() => {}}
                                          showModal
                                          deleteBookmark={() => {}}
                                          views={views} />);
      expect(wrapper.find('ModalDialog')).toMatchSnapshot();
    });

    it('should render with views', () => {
      const views = createViewsResponse(1);
      const wrapper = mount(<BookmarkList toggleModal={() => {}}
                                          showModal
                                          deleteBookmark={() => {}}
                                          views={views} />);
      expect(wrapper.find('ModalDialog')).toMatchSnapshot();
    });

    it('should handle toggleModal', () => {
      const views = createViewsResponse(1);
      const wrapper = mount(<BookmarkList toggleModal={() => {}}
                                          showModal
                                          deleteBookmark={() => {}}
                                          views={views} />);
      expect(wrapper.find('ModalDialog')).toMatchSnapshot();
    });

    it('should handle with toggle modal', () => {
      const onToggleModal = jest.fn();
      const views = createViewsResponse(1);

      const wrapper = mount(<BookmarkList toggleModal={onToggleModal}
                                          showModal
                                          deleteBookmark={() => {}}
                                          views={views} />);
      wrapper.find('button[children="Cancel"]').simulate('click');
      expect(onToggleModal).toBeCalledTimes(1);
    });

    it('should handle with delete', () => {
      window.confirm = jest.fn(() => true);
      const onDelete = jest.fn(() => {
        return new Promise(() => {});
      });
      const views = createViewsResponse(1);
      const wrapper = mount(<BookmarkList toggleModal={() => {}}
                                          showModal
                                          deleteBookmark={onDelete}
                                          views={views} />);
      wrapper.find('button.list-group-item').simulate('click');
      wrapper.find('button[children="Delete"]').simulate('click');
      expect(window.confirm).toBeCalledTimes(1);
      expect(onDelete).toBeCalledTimes(1);
    });

    it('should handle with load', () => {
      const onLoad = jest.fn(() => { return new Promise(() => {}); });
      const views = createViewsResponse(1);

      const wrapper = mount(
        <ViewLoaderContext.Provider value={onLoad}>
          <BookmarkList toggleModal={() => {}}
                        showModal
                        deleteBookmark={() => {}}
                        views={views} />
        </ViewLoaderContext.Provider>,
      );
      wrapper.find('button.list-group-item').simulate('click');
      wrapper.find('button[children="Load"]').simulate('click');
      expect(onLoad).toBeCalledTimes(1);
    });
  });
});
