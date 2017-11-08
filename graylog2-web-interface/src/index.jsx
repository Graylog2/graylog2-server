/* global REPLACE_MODULES */

// We need to set the app prefix before doing anything else, so it applies to styles too.
// eslint-disable-next-line no-unused-vars
import webpackEntry from 'webpack-entry';

import React from 'react';
import ReactDOM from 'react-dom';
import Promise from 'bluebird';
import Reflux from 'reflux';
import { AppContainer } from 'react-hot-loader';

Promise.config({ cancellation: true });
Reflux.setPromiseFactory(handlers => new Promise(handlers));

function renderAppContainer(appContainer) {
  // eslint-disable-next-line global-require
  const AppFacade = require('routing/AppFacade');
  ReactDOM.render(
    REPLACE_MODULES ? <AppContainer><AppFacade /></AppContainer> : <AppFacade />,
    appContainer,
  );
}

window.onload = () => {
  const appContainer = document.createElement('div');
  document.body.appendChild(appContainer);

  renderAppContainer(appContainer);

  if (module.hot && REPLACE_MODULES) {
    console.log('HMR enabled');
    module.hot.accept('routing/AppFacade', () => {
      renderAppContainer(appContainer);
    });
  }
};
