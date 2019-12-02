const SearchActions = {
  execute: {
    listen: jest.fn(),
    completed: {
      listen: jest.fn(),
    },
  },
};

// eslint-disable-next-line global-require
const loadSUT = () => require('./SearchLoadingStateStore');
jest.mock('views/logic/singleton', () => ({
  singletonActions: (key, target) => target(),
  singletonStore: (key, target) => target(),
}));
describe('SearchLoadingStateStore', () => {
  beforeEach(() => {
    jest.doMock('./SearchStore', () => ({ SearchActions }));
  });
  afterEach(() => {
    jest.resetAllMocks();
    jest.resetModules();
  });
  it('registers to SearchStore for search executions', () => {
    // eslint-disable-next-line no-unused-vars
    const { SearchLoadingStateStore } = loadSUT();
    expect(SearchActions.execute.listen).toHaveBeenCalledTimes(1);
    expect(SearchActions.execute.completed.listen).toHaveBeenCalledTimes(1);
  });

  it('initial state is indicating that no loading is in progress', () => {
    const { SearchLoadingStateStore } = loadSUT();

    expect(SearchLoadingStateStore.getInitialState()).toEqual({ isLoading: false });
  });

  it('sets state to loading when search is executed', (done) => {
    const { SearchLoadingStateStore } = loadSUT();
    SearchLoadingStateStore.listen(({ isLoading }) => {
      expect(isLoading).toBeTruthy();
      done();
    });

    const executeCallback = SearchActions.execute.listen.mock.calls[0][0];

    executeCallback();
  });

  it('sets state to be not loading when search is completed', (done) => {
    const { SearchLoadingStateStore } = loadSUT();
    const executeCallback = SearchActions.execute.listen.mock.calls[0][0];
    executeCallback();

    SearchLoadingStateStore.listen(({ isLoading }) => {
      expect(isLoading).toBeFalsy();
      done();
    });

    const executeCompletedCallback = SearchActions.execute.completed.listen.mock.calls[0][0];

    executeCompletedCallback();
  });
});
