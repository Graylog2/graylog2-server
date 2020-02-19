// We need to set the app prefix before doing anything else, so it applies to styles too.
import 'webpack-entry';

import React from 'react';
import ReactDOM from 'react-dom';
import Promise from 'bluebird';
import Reflux from 'reflux';

import AppFacade from 'routing/AppFacade';
import GraylogThemeProvider from 'theme/GraylogThemeProvider';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import ViewsBindings from 'views/bindings';

PluginStore.register(new PluginManifest({}, ViewsBindings));

Promise.config({ cancellation: true });
Reflux.setPromiseFactory(handlers => new Promise(handlers));

function renderAppContainer(appContainer) {
  ReactDOM.render(
    <GraylogThemeProvider>
      <AppFacade />
    </GraylogThemeProvider>,
    appContainer,
  );
}

window.onload = () => {
  const appContainer = document.createElement('div');
  document.body.appendChild(appContainer);

  renderAppContainer(appContainer);
};
