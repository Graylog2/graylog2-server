import { compose, createStore, applyMiddleware } from 'redux';
import { createLogger } from 'redux-logger';
import thunkMiddleware from 'redux-thunk';

import { getStateFromLocalStorage, persistStateToLocalStorage } from './persistedStateManager';

const loggerMiddleware = createLogger();
const composeEnhancers = window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose;

export default (reducer) => {
  const store = createStore(
    reducer,
    getStateFromLocalStorage(),
    composeEnhancers(
      applyMiddleware(
        thunkMiddleware,
        loggerMiddleware,
      ),
    ),
  );
  store.subscribe(() => persistStateToLocalStorage(store.getState()));

  return store;
};
