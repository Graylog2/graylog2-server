// @flow strict
import React from 'react';
import { mount } from 'enzyme';

import View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import BookmarkList from './BookmarkList';

describe('BookmarkList', () => {
  describe('render the BookmarkList', () => {
    it('should render empty', () => {
      const views = {
        pagination: {
          total: 0,
          page: 0,
          perPage: 5,
        },
        list: [],
      };
      const wrapper = mount(<BookmarkList toggleModal={() => {}}
                                          showModal
                                          deleteBookmark={() => {}}
                                          views={views} />);
      expect(wrapper.find('ModalDialog')).toMatchSnapshot();
    });

    it('should render with views', () => {
      const views = {
        pagination: {
          total: 0,
          page: 0,
          perPage: 5,
        },
        list: [
          View.builder()
            .id('foo-bar')
            .title('test')
            .description('desc')
            .build(),
        ],
      };
      const wrapper = mount(<BookmarkList toggleModal={() => {}}
                                          showModal
                                          deleteBookmark={() => {}}
                                          views={views} />);
      expect(wrapper.find('ModalDialog')).toMatchSnapshot();
    });

    it('should handle toggleModal', () => {
      const views = {
        pagination: {
          total: 0,
          page: 0,
          perPage: 5,
        },
        list: [],
      };
      const wrapper = mount(<BookmarkList toggleModal={() => {}}
                                          showModal
                                          deleteBookmark={() => {}}
                                          views={views} />);
      expect(wrapper.find('ModalDialog')).toMatchSnapshot();
    });

    it('should handle with toggle modal', () => {
      const onToggleModal = jest.fn();
      const views = {
        pagination: {
          total: 0,
          page: 0,
          perPage: 5,
        },
        list: [
          View.builder()
            .id('foo-bar')
            .title('test')
            .description('desc')
            .build(),
        ],
      };

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
      const views = {
        pagination: {
          total: 0,
          page: 0,
          perPage: 5,
        },
        list: [
          View.builder()
            .id('foo-bar')
            .title('test')
            .description('desc')
            .build(),
        ],
      };

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
      const views = {
        pagination: {
          total: 0,
          page: 0,
          perPage: 5,
        },
        list: [
          View.builder()
            .id('foo-bar')
            .title('test')
            .description('desc')
            .build(),
        ],
      };

      const wrapper = mount(
        <ViewLoaderContext.Provider value={{ loaderFunc: onLoad, dirty: false, loadedView: undefined }}>
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
