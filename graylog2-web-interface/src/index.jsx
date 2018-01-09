// We need to set the app prefix before doing anything else, so it applies to styles too.
import 'webpack-entry';

import React from 'react';
import ReactDOM from 'react-dom';
import Promise from 'bluebird';
import Reflux from 'reflux';
import { Provider } from 'mobx-react';
import DevTools from 'mobx-react-devtools';
import AppFacade from 'routing/AppFacade';
import AppConfig from 'util/AppConfig';
import RootStore from 'stores/RootStore';

Promise.config({ cancellation: true });
Reflux.setPromiseFactory(handlers => new Promise(handlers));

function renderAppContainer(appContainer) {
  ReactDOM.render(
    <Provider rootStore={RootStore}>
      <div>
        <AppFacade />
        {AppConfig.gl2DevMode() && <DevTools />}
      </div>
    </Provider>,
    appContainer,
  );
}

window.onload = () => {
  const appContainer = document.createElement('div');
  document.body.appendChild(appContainer);

  renderAppContainer(appContainer);
};
