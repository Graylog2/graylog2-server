// @flow strict
import React from 'react';
import { mount } from 'enzyme';

import View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import Search from 'views/logic/search/Search';
import BookmarkControls from './BookmarkControls';

describe('BookmarkControls', () => {
  describe('render the BookmarkControls', () => {
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

    it('should render with context', () => {
      const onLoad = jest.fn();
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
      const wrapper = mount(
        <ViewLoaderContext.Provider value={{ loaderFunc: onLoad, dirty: true, loadedView: view }}>
          <BookmarkControls viewStoreState={viewStoreState} />
        </ViewLoaderContext.Provider>,
      );
      expect(wrapper).toMatchSnapshot();
    });
  });
});
