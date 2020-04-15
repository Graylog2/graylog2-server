// @flow strict
import React from 'react';
import { mount } from 'wrappedEnzyme';
import { viewsManager, admin } from 'fixtures/users';

import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import mockAction from 'helpers/mocking/MockAction';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import * as Permissions from 'views/Permissions';
import CurrentUserContext from 'contexts/CurrentUserContext';

import SavedSearchControls from './SavedSearchControls';

describe('SavedSearchControls', () => {
  const createViewStoreState = (dirty = true, id) => ({
    activeQuery: '',
    view: View.builder()
      // $FlowFixMe: allowing `undefined` on purpose
      .id(id)
      .title('title')
      .description('description')
      .search(Search.create().toBuilder().id('id-beef').build())
      .owner('owningUser')
      .build(),
    dirty,
  });

  const mountSavedSearchControls = (loadNewView = () => Promise.resolve(), onLoadView = () => Promise.resolve(), currentUser = viewsManager) => (props) => mount(
    <ViewLoaderContext.Provider value={onLoadView}>
      <CurrentUserContext.Provider value={currentUser}>
        <NewViewLoaderContext.Provider value={loadNewView}>
          <SavedSearchControls viewStoreState={createViewStoreState()} {...props} />
        </NewViewLoaderContext.Provider>
      </CurrentUserContext.Provider>,
    </ViewLoaderContext.Provider>,
  );

  describe('Button handling', () => {
    it('should clear a view', (done) => {
      const loadNewView = jest.fn(() => {
        done();
        return Promise.resolve();
      });
      const wrapper = mountSavedSearchControls(loadNewView)();
      wrapper.find('a[data-testid="reset-search"]').simulate('click');
    });

    it('should loadView after create', (done) => {
      ViewManagementActions.create = mockAction(jest.fn((view) => Promise.resolve(view)));
      const onLoadView = jest.fn((view) => {
        return new Promise(() => view);
      });
      const wrapper = mountSavedSearchControls(undefined, onLoadView)({ viewStoreState: createViewStoreState(false) });
      wrapper.find('button[title="Save search"]').simulate('click');
      wrapper.find('input[value="title"]').simulate('change', { target: { value: 'Test' } });
      wrapper.find('button[children="Create new"]').simulate('click');
      setImmediate(() => {
        expect(onLoadView).toHaveBeenCalledTimes(1);
        done();
      });
    });

    describe('has "Share search" option', () => {
      it('includes the option to share the current search', () => {
        const wrapper = mountSavedSearchControls()({ viewStoreState: createViewStoreState(false, 'some-id') });

        expect(wrapper.find('MenuItem[title="Share search"]')).toExist();
      });

      it('which should be disabled if current user is neither owner nor permitted to edit search', () => {
        const wrapper = mountSavedSearchControls()({ viewStoreState: createViewStoreState(false, 'some-id') });

        const shareSearch = wrapper.find('MenuItem[title="Share search"]');

        expect(shareSearch).toBeDisabled();
      });

      it('which should be enabled if current user is owner of search', () => {
        const owningUser = {
          ...viewsManager,
          username: 'owningUser',
          permissions: [],
        };
        const wrapper = mountSavedSearchControls(undefined, undefined, owningUser)({ viewStoreState: createViewStoreState(false, owningUser.id) });

        const shareSearch = wrapper.find('MenuItem[title="Share search"]');

        expect(shareSearch).not.toBeDisabled();
      });
      it('which should be enabled if current user is permitted to edit search', () => {
        const owningUser = {
          ...viewsManager,
          username: 'powerfulUser',
          permissions: [Permissions.View.Edit(viewsManager.id)],
        };
        const wrapper = mountSavedSearchControls(undefined, undefined, owningUser)({ viewStoreState: createViewStoreState(false, owningUser.id) });

        const shareSearch = wrapper.find('MenuItem[title="Share search"]');

        expect(shareSearch).not.toBeDisabled();
      });
      it('which should be enabled if current user is admin', () => {
        const wrapper = mountSavedSearchControls(undefined, undefined, admin)({ viewStoreState: createViewStoreState(false, admin.id) });

        const shareSearch = wrapper.find('MenuItem[title="Share search"]');

        expect(shareSearch).not.toBeDisabled();
      });
      it('which should be disabled if search is unsaved', () => {
        const wrapper = mountSavedSearchControls()();

        const shareSearch = wrapper.find('MenuItem[title="Share search"]');

        expect(shareSearch).toBeDisabled();
      });
    });
  });

  describe('render the SavedSearchControls', () => {
    it('should render not dirty with unsaved view', () => {
      const wrapper = mountSavedSearchControls()({ viewStoreState: createViewStoreState(false) });

      const saveButton = wrapper.find('button[title="Save search"]');
      expect(saveButton).toMatchSnapshot();
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
      const wrapper = mountSavedSearchControls()({ viewStoreState });
      const saveButton = wrapper.find('button[title="Saved search"]');
      expect(saveButton).toMatchSnapshot();
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
      const wrapper = mountSavedSearchControls()({ viewStoreState });
      const saveButton = wrapper.find('button[title="Unsaved changes"]');
      expect(saveButton).toMatchSnapshot();
    });
  });
});
