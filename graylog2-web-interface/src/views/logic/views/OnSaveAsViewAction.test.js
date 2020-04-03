import View from './View';

// eslint-disable-next-line global-require
const loadSUT = () => require('./OnSaveAsViewAction');

const mockActions = () => {
  const ViewActions = { load: jest.fn(() => Promise.resolve({ view: { id: 'deadbeef' } })).mockName('load') };
  const ViewManagementActions = {
    create: jest.fn((v) => Promise.resolve(v)).mockName('create'),
  };
  jest.doMock('views/stores/ViewManagementStore', () => ({ ViewManagementActions }));
  jest.doMock('views/stores/ViewStore', () => ({ ViewActions }));

  return { ViewActions, ViewManagementActions };
};

describe('OnSaveAsViewAction', () => {
  afterEach(() => {
    jest.resetModules();
  });
  it('saves a given new view', () => {
    const { ViewManagementActions } = mockActions();
    const onSaveAsView = loadSUT();
    const view = View.create();
    const router = [];

    return onSaveAsView(view, router).then(() => {
      expect(ViewManagementActions.create).toHaveBeenCalledTimes(1);
      expect(ViewManagementActions.create.mock.calls[0][0]).toEqual(view);
    });
  });

  it('loads saved view', () => {
    const { ViewActions } = mockActions();
    const onSaveAsView = loadSUT();
    const view = View.create();
    const router = [];

    return onSaveAsView(view, router).then(() => {
      expect(ViewActions.load).toHaveBeenCalledTimes(1);
      expect(ViewActions.load.mock.calls[0][0]).toEqual(view);
    });
  });

  it('redirects to saved view', () => {
    mockActions();
    const onSaveAsView = loadSUT();
    const view = View.create();
    const router = [];

    return onSaveAsView(view, router).then(() => {
      expect(router).toHaveLength(1);
      expect(router).toEqual(['/dashboards/deadbeef']);
    });
  });

  it('shows notification upon success', () => {
    mockActions();
    const UserNotification = { success: jest.fn().mockName('success') };
    jest.doMock('util/UserNotification', () => UserNotification);
    const onSaveAsView = loadSUT();
    const view = View.create().toBuilder().title('Test View').build();
    const router = [];

    return onSaveAsView(view, router).then(() => {
      expect(UserNotification.success).toHaveBeenCalledTimes(1);
      expect(UserNotification.success.mock.calls[0][0]).toEqual(`Saving view "${view.title}" was successful!`);
      expect(UserNotification.success.mock.calls[0][1]).toEqual('Success!');
    });
  });

  it('does not do anything if saving fails', () => {
    const { ViewManagementActions, ViewActions } = mockActions();
    ViewManagementActions.create = jest.fn(() => Promise.reject(new Error('Something bad happened!')));
    const UserNotification = {
      success: jest.fn().mockName('success'),
      error: jest.fn().mockName('error'),
    };
    jest.doMock('util/UserNotification', () => UserNotification);

    const onSaveAsView = loadSUT();
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
