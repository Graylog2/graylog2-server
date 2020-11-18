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
