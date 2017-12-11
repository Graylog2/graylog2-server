// We need to set the app prefix before doing anything else, so it applies to styles too.
import 'webpack-entry';

import React from 'react';
import ReactDOM from 'react-dom';
import Promise from 'bluebird';
import Reflux from 'reflux';
import { Provider } from 'react-redux';

import AppFacade from 'routing/AppFacade';
import { combineReducers } from 'redux';
import configureStore from 'stores/configureStore';
import systemReducer from 'ducks/system';
import jvmReducer from 'ducks/jvm';

Promise.config({ cancellation: true });
Reflux.setPromiseFactory(handlers => new Promise(handlers));

function renderAppContainer(appContainer) {
  ReactDOM.render(
    <Provider store={configureStore(combineReducers({ system: systemReducer, jvm: jvmReducer }))}>
      <AppFacade />
    </Provider>,
    appContainer,
  );
}

window.onload = () => {
  const appContainer = document.createElement('div');
  document.body.appendChild(appContainer);

  renderAppContainer(appContainer);
};
