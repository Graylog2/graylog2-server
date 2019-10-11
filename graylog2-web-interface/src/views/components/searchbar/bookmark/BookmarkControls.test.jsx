// @flow strict
import React from 'react';
import { mount } from 'enzyme';

import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import mockAction from 'helpers/mocking/MockAction';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import { ViewActions } from 'views/stores/ViewStore';

import BookmarkControls from './BookmarkControls';

describe('BookmarkControls', () => {
  describe('Button handling', () => {
    it('should clear a view', (done) => {
      ViewActions.create = mockAction(jest.fn(() => Promise.resolve()));
      const viewStoreState = {
        activeQuery: '',
        view: View.builder()
          .title('title')
          .description('description')
          .search(Search.create().toBuilder().id('id-beef').build())
          .build(),
        dirty: true,
      };
      const wrapper = mount(<BookmarkControls viewStoreState={viewStoreState} />);
      wrapper.find('button[title="Empty search"]').simulate('click');
      setImmediate(() => {
        expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search);
        done();
      });
    });

    it('should loadView after create', (done) => {
      ViewManagementActions.create = mockAction(jest.fn(() => Promise.resolve()));
      const onLoadView = jest.fn(() => {
        return Promise.resolve();
      });
      const viewStoreState = {
        activeQuery: '',
        view: View.builder()
          .title('title')
          .description('description')
          .search(Search.create().toBuilder().id('id-beef').build())
          .build(),
        dirty: false,
      };
      const wrapper = mount(
        <ViewLoaderContext.Provider value={onLoadView}>
          <BookmarkControls viewStoreState={viewStoreState} />
        </ViewLoaderContext.Provider>,
      );
      wrapper.find('button[title="Save search"]').simulate('click');
      wrapper.find('input[value="title"]').simulate('change', { target: { value: 'Test' } });
      wrapper.find('button[children="Create new"]').simulate('click');
      setImmediate(() => {
        expect(onLoadView).toHaveBeenCalledTimes(1);
        done();
      });
    });
  });

  describe('render the BookmarkControls', () => {
    it('should render not dirty with unsaved view', () => {
      const viewStoreState = {
        activeQuery: '',
        view: View.builder()
          .title('title')
          .description('description')
          .search(Search.create().toBuilder().id('id-beef').build())
          .build(),
        dirty: false,
      };
      const wrapper = mount(<BookmarkControls viewStoreState={viewStoreState} />);
      expect(wrapper).toMatchSnapshot();
    });

    it('should render not dirty', () => {
      const viewStoreState = {
        activeQuery: '',
        view: View.builder()
          .title('title')
          .description('description')
          .search(Search.create().toBuilder().id('id-beef').build())
          .id('id-beef')
          .build(),
        dirty: false,
      };
      const wrapper = mount(<BookmarkControls viewStoreState={viewStoreState} />);
      expect(wrapper).toMatchSnapshot();
    });

    it('should render dirty', () => {
      const view = View.builder()
        .title('title')
        .description('description')
        .search(Search.create().toBuilder().id('id-beef').build())
        .id('id-beef')
        .build();
      const viewStoreState = {
        activeQuery: '',
        view: view,
        dirty: true,
      };
      const wrapper = mount(<BookmarkControls viewStoreState={viewStoreState} />);
      expect(wrapper).toMatchSnapshot();
    });
  });
});
