// @flow strict

import type { ListenableAction } from 'stores/StoreTypes';

const listenable = () => ({ listen: jest.fn(() => jest.fn()) });

const noop: function = jest.fn();

const mockAction = <R: function>(fn: R = noop): ListenableAction<R> => {
  return Object.assign(fn, listenable(), {
    completed: listenable(),
    promise: jest.fn(),
  });
};

export default mockAction;
