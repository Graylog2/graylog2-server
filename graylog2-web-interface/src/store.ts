import { configureStore } from '@reduxjs/toolkit';
import type { PluggableReducer } from 'graylog-web-plugin';

import type { RootState } from 'views/types';

const createStore = (reducers: PluggableReducer[], initialState: RootState) => {
  const reducer = Object.fromEntries(reducers.map((r) => [r.key, r.reducer]));

  return configureStore({
    reducer,
    preloadedState: initialState,
  });
};

export default createStore;
