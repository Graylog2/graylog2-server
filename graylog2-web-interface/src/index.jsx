const AppConfig = require('util/AppConfig');

// This needs to be set as early as possible to avoid problems with loading
// CSS. If this is set after requiring AppFacade, everything required in the
// AppFacade (like CSS) does not get the app path prefix and fails to load
// if the URL is not /. (i.e. entering /system/overview in the browser
// address bar works but the font-awesome icons are not loaded)
__webpack_public_path__ = AppConfig.gl2AppPathPrefix() + '/';

// We have to use require() here to ensure that __webpack_public_path__ is set
// first. When using import the __webpack_public_path__ setting is moved after
// all imports...
const React = require('react');
const ReactDOM = require('react-dom');
const AppFacade = require('routing/AppFacade');
const Promise = require('bluebird');
const Reflux = require('reflux');

Reflux.setPromiseFactory((handlers) => new Promise(handlers));

window.onload = () => {
  const appContainer = document.createElement('div');
  document.body.appendChild(appContainer);
  ReactDOM.render(<AppFacade />, appContainer);
};
