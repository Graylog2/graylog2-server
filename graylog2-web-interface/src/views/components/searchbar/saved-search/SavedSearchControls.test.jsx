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
// @flow strict
import React from 'react';
import { mount } from 'wrappedEnzyme';
import { viewsManager, admin } from 'fixtures/users';
import mockAction from 'helpers/mocking/MockAction';

import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import ViewLoaderContext, { type ViewLoaderContextType } from 'views/logic/ViewLoaderContext';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import NewViewLoaderContext, { type NewViewLoaderContextType } from 'views/logic/NewViewLoaderContext';
import * as Permissions from 'views/Permissions';
import CurrentUserContext from 'contexts/CurrentUserContext';
import type { UserJSON } from 'logic/users/User';
import type { ViewStoreState } from 'views/stores/ViewStore';

import SavedSearchControls from './SavedSearchControls';

describe('SavedSearchControls', () => {
  const createViewStoreState = (dirty = true, id) => ({
    activeQuery: '',
    view: View.builder()
      // $FlowFixMe: allowing `undefined` on purpose
      .id(id)
      .title('title')
      .type(View.Type.Search)
      .description('description')
      .search(Search.create().toBuilder().id('id-beef').build())
      .owner('owningUser')
      .build(),
    dirty,
    isNew: false,
  });

  type SimpleSavedSearchControlsProps = {
    loadNewView?: NewViewLoaderContextType,
    onLoadView?: ViewLoaderContextType,
    currentUser?: UserJSON,
    viewStoreState?: ViewStoreState,
  };

  const SimpleSavedSearchControls = ({ loadNewView = () => Promise.resolve(), onLoadView, currentUser, ...props }: SimpleSavedSearchControlsProps) => (
    <ViewLoaderContext.Provider value={onLoadView}>
      <CurrentUserContext.Provider value={currentUser}>
        <NewViewLoaderContext.Provider value={loadNewView}>
          <SavedSearchControls {...props} />
        </NewViewLoaderContext.Provider>
      </CurrentUserContext.Provider>
    </ViewLoaderContext.Provider>
  );

  SimpleSavedSearchControls.defaultProps = {
    loadNewView: () => Promise.resolve(),
    onLoadView: () => Promise.resolve(),
    currentUser: viewsManager,
    viewStoreState: createViewStoreState(),
  };

  describe('Button handling', () => {
    it('should clear a view', (done) => {
      const loadNewView = jest.fn(() => {
        done();

        return Promise.resolve();
      });
      const wrapper = mount(<SimpleSavedSearchControls loadNewView={loadNewView} />);

      wrapper.find('a[data-testid="reset-search"]').simulate('click');
    });

    it('should loadView after create', (done) => {
      ViewManagementActions.create = mockAction(jest.fn((view) => Promise.resolve(view)));
      const onLoadView = jest.fn((view) => {
        return new Promise(() => view);
      });
      const wrapper = mount(<SimpleSavedSearchControls onLoadView={onLoadView} viewStoreState={createViewStoreState(false)} />);

      wrapper.find('button[title="Save search"]').simulate('click');
      wrapper.find('input[value="title"]').simulate('change', { target: { value: 'Test' } });
      wrapper.find('button[children="Create new"]').simulate('click');

      setImmediate(() => {
        expect(onLoadView).toHaveBeenCalledTimes(1);

        done();
      });
    });

    describe('has "Share" option', () => {
      it('includes the option to share the current search', () => {
        const wrapper = mount(<SimpleSavedSearchControls viewStoreState={createViewStoreState(false, 'some-id')} />);

        expect(wrapper.find('button[title="Share"]')).toExist();
      });

      it('which should be disabled if current user is neither owner nor permitted to edit search', () => {
        const notOwningUser = {
          ...viewsManager,
          username: 'notOwningUser',
          permissions: [],
          grn_permissions: [],
        };
        const wrapper = mount(<SimpleSavedSearchControls currentUser={notOwningUser} viewStoreState={createViewStoreState(false, 'some-id')} />);

        const shareSearch = wrapper.find('button[title="Share"]');

        expect(shareSearch).toBeDisabled();
      });

      it('which should be enabled if current user is owner of search', () => {
        const owningUser = {
          ...viewsManager,
          username: 'owningUser',
          permissions: [],
          grn_permissions: ['entity:own:grn::::search:user-id-1'],
        };
        const wrapper = mount(<SimpleSavedSearchControls currentUser={owningUser} viewStoreState={createViewStoreState(false, owningUser.id)} />);
        const shareSearch = wrapper.find('button[title="Share"]');

        expect(shareSearch).not.toBeDisabled();
      });

      it('which should be enabled if current user is permitted to edit search', () => {
        const owningUser = {
          ...viewsManager,
          username: 'powerfulUser',
          permissions: [Permissions.View.Edit(viewsManager.id)],
          grn_permissions: ['entity:own:grn::::search:user-id-1'],
        };

        const wrapper = mount(<SimpleSavedSearchControls currentUser={owningUser} viewStoreState={createViewStoreState(false, owningUser.id)} />);

        const shareSearch = wrapper.find('button[title="Share"]');

        expect(shareSearch).not.toBeDisabled();
      });

      it('which should be enabled if current user is admin', () => {
        const wrapper = mount(<SimpleSavedSearchControls currentUser={admin} viewStoreState={createViewStoreState(false, admin.id)} />);

        const shareSearch = wrapper.find('button[title="Share"]');

        expect(shareSearch).not.toBeDisabled();
      });

      it('which should be hidden if search is unsaved', () => {
        const wrapper = mount(<SimpleSavedSearchControls />);

        const shareSearch = wrapper.find('button[title="Share"]');

        expect(shareSearch).toMatchSnapshot();
      });
    });
  });

  describe('render the SavedSearchControls', () => {
    it('should render not dirty with unsaved view', () => {
      const wrapper = mount(<SimpleSavedSearchControls viewStoreState={createViewStoreState(false)} />);

      const saveButton = wrapper.find('button[title="Save search"]');

      expect(saveButton).toExist();
    });

    it('should render not dirty', () => {
      const viewStoreState = {
        activeQuery: '',
        view: View.builder()
          .title('title')
          .description('description')
          .type(View.Type.Search)
          .search(Search.create().toBuilder().id('id-beef').build())
          .id('id-beef')
          .build(),
        dirty: false,
        isNew: true,
      };
      const wrapper = mount(<SimpleSavedSearchControls viewStoreState={viewStoreState} />);
      const saveButton = wrapper.find('button[title="Saved search"]');

      expect(saveButton).toExist();
    });

    it('should render dirty', () => {
      const view = View.builder()
        .title('title')
        .type(View.Type.Search)
        .description('description')
        .search(Search.create().toBuilder().id('id-beef').build())
        .id('id-beef')
        .build();
      const viewStoreState = {
        activeQuery: '',
        view: view,
        dirty: true,
        isNew: false,
      };
      const wrapper = mount(<SimpleSavedSearchControls viewStoreState={viewStoreState} />);
      const saveButton = wrapper.find('button[title="Unsaved changes"]');

      expect(saveButton).toExist();
    });
  });
});
