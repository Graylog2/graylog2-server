/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// We need to set the app prefix before doing anything else, so it applies to styles too.
import 'webpack-entry';

import React from 'react';
import ReactDOM from 'react-dom';
import Promise from 'bluebird';
import Reflux from 'reflux';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import AppFacade from 'routing/AppFacade';
import GraylogThemeProvider from 'theme/GraylogThemeProvider';
import CustomizationProvider from 'contexts/CustomizationProvider';
import ViewsBindings from 'views/bindings';
import GlobalThemeStyles from 'theme/GlobalThemeStyles';

PluginStore.register(new PluginManifest({}, ViewsBindings));

Promise.config({ cancellation: true });
Reflux.setPromiseFactory((handlers) => new Promise(handlers));

function renderAppContainer(appContainer) {
  ReactDOM.render(
    <CustomizationProvider>
      <GraylogThemeProvider>
        <GlobalThemeStyles />
        <AppFacade />
      </GraylogThemeProvider>
    </CustomizationProvider>,
    appContainer,
  );
}

window.onload = () => {
  const appContainer = document.createElement('div');

  appContainer.id = 'app-root';

  document.body.appendChild(appContainer);

  renderAppContainer(appContainer);
};
