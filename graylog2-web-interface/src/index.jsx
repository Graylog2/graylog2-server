// We need to set the app prefix before doing anything else, so it applies to styles too.
import 'webpack-entry';

import React from 'react';
import ReactDOM from 'react-dom';
import Promise from 'bluebird';
import Reflux from 'reflux';
import { Provider } from 'react-redux';

import AppFacade from 'routing/AppFacade';
import configureStore from 'stores/configureStore';
import systemReducers from 'ducks/system/index';

Promise.config({ cancellation: true });
Reflux.setPromiseFactory(handlers => new Promise(handlers));

function renderAppContainer(appContainer) {
  ReactDOM.render(
    <Provider store={configureStore(systemReducers)}>
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
