import UserNotification from 'util/UserNotification';
import { ViewActions } from 'views/stores/ViewStore';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';

import View from './View';
import onSaveAsView from './OnSaveAsViewAction';

jest.mock('routing/Routes', () => ({ VIEWS: { VIEWID: viewId => `/views/${viewId}` } }));

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    create: jest.fn(() => Promise.resolve()).mockName('create'),
  },
}));

jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {
    load: jest.fn(() => Promise.resolve({ view: { id: 'deadbeef' } })).mockName('load'),
  },
}));

jest.mock('util/UserNotification', () => ({
  success: jest.fn().mockName('success'),
  error: jest.fn().mockName('error'),
}));


describe('OnSaveAsViewAction', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('saves a given new view', () => {
    const view = View.create();
    const router = [];

    return onSaveAsView(view, router).then(() => {
      expect(ViewManagementActions.create).toHaveBeenCalledTimes(1);
      expect(ViewManagementActions.create.mock.calls[0][0]).toEqual(view);
    });
  });

  it('loads saved view', () => {
    const view = View.create();
    const router = [];

    return onSaveAsView(view, router).then(() => {
      expect(ViewActions.load).toHaveBeenCalledTimes(1);
      expect(ViewActions.load.mock.calls[0][0]).toEqual(view);
    });
  });

  it('redirects to saved view', () => {
    const view = View.create();
    const router = [];

    return onSaveAsView(view, router).then(() => {
      expect(router).toHaveLength(1);
      expect(router).toEqual(['/views/deadbeef']);
    });
  });

  it('shows notification upon success', () => {
    const view = View.create().toBuilder().title('Test View').build();
    const router = [];

    return onSaveAsView(view, router).then(() => {
      expect(UserNotification.success).toHaveBeenCalledTimes(1);
      expect(UserNotification.success.mock.calls[0][0]).toEqual(`Saving view "${view.title}" was successful!`);
      expect(UserNotification.success.mock.calls[0][1]).toEqual('Success!');
    });
  });

  it('does not do anything if saving fails', () => {
    ViewManagementActions.create.mockImplementation(() => Promise.reject(new Error('Something bad happened!')));

    const view = View.create();
    const router = [];

    return onSaveAsView(view, router).then(() => {
      expect(ViewActions.load).not.toHaveBeenCalled();
      expect(router).toEqual([]);
      expect(UserNotification.success).not.toHaveBeenCalled();
      expect(UserNotification.error).toHaveBeenCalledTimes(1);
      expect(UserNotification.error.mock.calls[0][0]).toEqual('Saving view failed: Error: Something bad happened!');
      expect(UserNotification.error.mock.calls[0][1]).toEqual('Error!');
    });
  });
});
