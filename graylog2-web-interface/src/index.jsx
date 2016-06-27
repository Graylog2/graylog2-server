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
