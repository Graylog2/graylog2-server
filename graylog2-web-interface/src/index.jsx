import React from 'react';
import ReactDOM from 'react-dom';
import AppFacade from 'routing/AppFacade';
import AppConfig from 'util/AppConfig';
import Promise from 'bluebird';
import Reflux from 'reflux';

console.log("Setting public path to: ", AppConfig.applicationPrefix());
__webpack_public_path__ = AppConfig.gl2AppPathPrefix() + '/';

Reflux.setPromiseFactory((handlers) => new Promise(handlers));

window.onload = () => {
  const appContainer = document.createElement('div');
  document.body.appendChild(appContainer);
  ReactDOM.render(<AppFacade />, appContainer);
};
