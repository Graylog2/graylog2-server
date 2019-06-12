import View from './View';

jest.mock('routing/Routes', () => ({ VIEWS: { VIEWID: viewId => `/views/${viewId}` } }));

// eslint-disable-next-line global-require
const loadSUT = () => require('./OnSaveViewAction');

const mockActions = () => {
  const ViewActions = { load: jest.fn(() => Promise.resolve({ view: { id: 'deadbeef' } })).mockName('load') };
  const ViewManagementActions = {
    update: jest.fn(() => Promise.resolve()).mockName('update'),
  };
  jest.doMock('views/stores/ViewManagementStore', () => ({ ViewManagementActions }));
  jest.doMock('views/stores/ViewStore', () => ({ ViewActions }));

  return { ViewActions, ViewManagementActions };
};

describe('OnSaveViewAction', () => {
  afterEach(() => {
    jest.resetModules();
  });
  it('saves a given view', () => {
    const { ViewManagementActions } = mockActions();
    const onSaveView = loadSUT();
    const view = View.create();
    const router = [];

    return onSaveView(view, router).then(() => {
      expect(ViewManagementActions.update).toHaveBeenCalledTimes(1);
      expect(ViewManagementActions.update.mock.calls[0][0]).toEqual(view);
    });
  });

  it('does not load saved view', () => {
    const { ViewActions } = mockActions();
    const onSaveView = loadSUT();
    const view = View.create();
    const router = [];

    return onSaveView(view, router).then(() => {
      expect(ViewActions.load).not.toHaveBeenCalled();
    });
  });

  it('does not redirect to saved view', () => {
    mockActions();
    const onSaveView = loadSUT();
    const view = View.create();
    const router = [];

    return onSaveView(view, router).then(() => {
      expect(router).toHaveLength(0);
      expect(router).toEqual([]);
    });
  });

  it('shows notification upon success', () => {
    mockActions();
    const UserNotification = { success: jest.fn().mockName('success') };
    jest.doMock('util/UserNotification', () => UserNotification);
    const onSaveView = loadSUT();
    const view = View.create().toBuilder().title('Test View').build();
    const router = [];

    return onSaveView(view, router).then(() => {
      expect(UserNotification.success).toHaveBeenCalledTimes(1);
      expect(UserNotification.success.mock.calls[0][0]).toEqual(`Saving view "${view.title}" was successful!`);
      expect(UserNotification.success.mock.calls[0][1]).toEqual('Success!');
    });
  });

  it('does not do anything if saving fails', () => {
    const { ViewManagementActions } = mockActions();
    ViewManagementActions.update = jest.fn(() => Promise.reject('Something bad happened!'));
    const UserNotification = {
      success: jest.fn().mockName('success'),
      error: jest.fn().mockName('error'),
    };
    jest.doMock('util/UserNotification', () => UserNotification);

    const onSaveView = loadSUT();
    const view = View.create();
    const router = [];

    return onSaveView(view, router).then(() => {
      expect(router).toEqual([]);
      expect(UserNotification.success).not.toHaveBeenCalled();
      expect(UserNotification.error).toHaveBeenCalledTimes(1);
      expect(UserNotification.error.mock.calls[0][0]).toEqual('Saving view failed: Something bad happened!');
      expect(UserNotification.error.mock.calls[0][1]).toEqual('Error!');
    });
  });
});
