import { configureStore } from '@reduxjs/toolkit';
import type { PluggableReducer } from 'graylog-web-plugin';

const createStore = (reducers: PluggableReducer[]) => {
  const reducer = Object.fromEntries(reducers.map((r) => [r.key, r.reducer]));

  return configureStore({
    reducer,
  });
};

export default createStore;
