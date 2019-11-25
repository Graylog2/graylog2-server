// @flow strict

import type { ListenableAction } from 'stores/StoreTypes';

// eslint-disable-next-line arrow-parens
const mockAction = <R: function>(fn: R): ListenableAction<R> => {
  return Object.assign(fn, {
    listen: jest.fn(() => jest.fn()),
    completed: { listen: jest.fn(() => jest.fn()) },
    promise: jest.fn(),
  });
};

export default mockAction;
