// We need to set the app prefix before doing anything else, so it applies to styles too.
// eslint-disable-next-line no-unused-vars
import webpackEntry from 'webpack-entry';

import React from 'react';
import ReactDOM from 'react-dom';
import AppFacade from 'routing/AppFacade';
import Promise from 'bluebird';
import Reflux from 'reflux';

Reflux.setPromiseFactory((handlers) => new Promise(handlers));

window.onload = () => {
  const appContainer = document.createElement('div');
  document.body.appendChild(appContainer);
  ReactDOM.render(<AppFacade />, appContainer);
};
