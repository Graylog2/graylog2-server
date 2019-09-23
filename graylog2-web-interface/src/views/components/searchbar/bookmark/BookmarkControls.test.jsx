// @flow strict
import React from 'react';
import { mount } from 'enzyme';

import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import BookmarkControls from './BookmarkControls';

describe('BookmarkControls', () => {
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
