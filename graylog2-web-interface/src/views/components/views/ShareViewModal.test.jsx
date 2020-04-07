// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import { ViewSharingActions } from 'views/stores/ViewSharingStore';
import AllUsersOfInstance from 'views/logic/views/sharing/AllUsersOfInstance';
import ViewSharing from 'views/logic/views/sharing/ViewSharing';
import View from 'views/logic/views/View';
import ShareViewModal from './ShareViewModal';

const mockLoadRoles = jest.fn(() => Promise.resolve([]));

ViewSharing.registerSubtype(AllUsersOfInstance.Type, AllUsersOfInstance);

jest.mock('stores/connect', () => (x) => x);
jest.mock('injection/StoreProvider', () => ({
  getStore: (store) => {
    switch (store) {
      case 'Roles':
        return {
          loadRoles: () => mockLoadRoles(),
        };
      case 'CurrentUser':
        return {};
      default:
        return null;
    }
  },
}));
jest.mock('views/stores/ViewSharingStore', () => ({
  ViewSharingActions: {
    create: jest.fn(() => Promise.resolve()),
    get: jest.fn(() => Promise.resolve()),
    remove: jest.fn(() => Promise.resolve()),
    users: jest.fn(() => Promise.resolve()),
  },
}));

describe('ShareViewModal', () => {
  const view = View.builder()
    .id('deadbeef')
    .title('My fabulous view')
    .type(View.Type.Search)
    .build();
  const currentUser = { roles: [], permissions: [] };
  const onClose = jest.fn();

  afterEach(() => {
    jest.clearAllMocks();
  });
  describe('upon mount', () => {
    it('retrieves view sharing', () => {
      mount(<ShareViewModal show view={view} currentUser={currentUser} onClose={onClose} />);

      expect(ViewSharingActions.get).toHaveBeenCalledWith(view.id);
    });
    it('retrieves list of users available for sharing', () => {
      mount(<ShareViewModal show view={view} currentUser={currentUser} onClose={onClose} />);

      expect(ViewSharingActions.users).toHaveBeenCalledWith(view.id);
    });
    it('retrieves list of users\' roles', () => {
      mount(<ShareViewModal show view={view} currentUser={currentUser} onClose={onClose} />);

      expect(mockLoadRoles).not.toHaveBeenCalled();
    });
    it('retrieves list of all roles if user is admin', () => {
      const admin = { roles: ['Admin'] };
      mount(<ShareViewModal show view={view} currentUser={admin} onClose={onClose} />);

      expect(mockLoadRoles).toHaveBeenCalled();
    });
  });
  it('renders four sharing options', (done) => {
    const wrapper = mount(<ShareViewModal show view={view} currentUser={currentUser} onClose={onClose} />);
    setImmediate(() => {
      wrapper.update();
      expect(wrapper.find('input[type="radio"]')).toHaveLength(4);
      done();
    });
  });
  it('selects "Only Me" if no view sharing is present', (done) => {
    const wrapper = mount(<ShareViewModal show view={view} currentUser={currentUser} onClose={onClose} />);
    setImmediate(() => {
      wrapper.update();
      expect(wrapper.find('input[name="none"]')).toHaveProp('checked', true);
      done();
    });
  });
  it('does not do anything on cancel', (done) => {
    const wrapper = mount(<ShareViewModal show view={view} currentUser={currentUser} onClose={onClose} />);
    setImmediate(() => {
      wrapper.update();
      const button = wrapper.find('button[children="Cancel"]');
      button.simulate('click');

      expect(ViewSharingActions.create).not.toHaveBeenCalled();
      expect(ViewSharingActions.remove).not.toHaveBeenCalled();
      done();
    });
  });
  it('removes view sharing if saved with "Only Me" selected', (done) => {
    const wrapper = mount(<ShareViewModal show view={view} currentUser={currentUser} onClose={onClose} />);
    setImmediate(() => {
      wrapper.update();
      const button = wrapper.find('button[children="Save"]');
      button.simulate('click');

      expect(ViewSharingActions.create).not.toHaveBeenCalled();
      expect(ViewSharingActions.remove).toHaveBeenCalledWith(view.id);
      done();
    });
  });
  it('creates view sharing if saved with other option selected', (done) => {
    const wrapper = mount(<ShareViewModal show view={view} currentUser={currentUser} onClose={onClose} />);
    setImmediate(() => {
      wrapper.update();
      const allUsersOfInstanceRadio = wrapper.find('input[name="all_of_instance"]');
      allUsersOfInstanceRadio.simulate('change', { target: { name: 'all_of_instance' } });
      const button = wrapper.find('button[children="Save"]');
      button.simulate('click');

      expect(ViewSharingActions.create).toHaveBeenCalledWith(view.id, AllUsersOfInstance.create(view.id));
      expect(ViewSharingActions.remove).not.toHaveBeenCalled();
      done();
    });
  });
  it('displays correct description if view is a search', () => {
    const wrapper = mount(<ShareViewModal show view={view} currentUser={currentUser} onClose={onClose} />);
    expect(wrapper.contains('Who is supposed to access the search My fabulous view?')).toBe(true);
  });
  it('displays correct description if view is a dashboard', () => {
    const dashboardView = view.toBuilder().type(View.Type.Dashboard).build();
    const wrapper = mount(<ShareViewModal show view={dashboardView} currentUser={currentUser} onClose={onClose} />);
    expect(wrapper.contains('Who is supposed to access the dashboard My fabulous view?')).toBe(true);
  });
});
