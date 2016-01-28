// This needs to be imported before Reflux (or components importing it), in order to polyfill
// the Promise object if it is not available natively.
import {} from 'util/PromiseShim';

import React from 'react';
import ReactDOM from 'react-dom';
import AppFacade from 'routing/AppFacade';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

window.onload = () => {
  const appContainer = document.createElement('div');
  document.body.appendChild(appContainer);
  ReactDOM.render(<AppFacade
    storeProvider={new StoreProvider()}
    actionsProvider={new ActionsProvider()}
  />, appContainer);
};
