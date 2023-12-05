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
import Reflux from 'reflux';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import AppFacade from 'routing/AppFacade';
import GraylogThemeProvider from 'theme/GraylogThemeProvider';
import CustomizationProvider from 'contexts/CustomizationProvider';
import ViewsBindings from 'views/bindings';
import ThreatIntelBindings from 'threatintel/bindings';
import AwsBindings from 'aws/bindings';
import IntegrationsBindings from 'integrations/bindings';
import GlobalThemeStyles from 'theme/GlobalThemeStyles';
import CancellablePromise from 'logic/rest/CancellablePromise';
import TelemetryInit from 'logic/telemetry/TelemetryInit';
import LoginQueryClientProvider from 'contexts/LoginQueryClientProvider';
import PerspectivesBindings from 'components/perspectives/bindings';
import NavigationBindings from 'components/navigation/bindings';

Reflux.setPromiseFactory((handlers) => CancellablePromise.of(new Promise(handlers)));

PluginStore.register(new PluginManifest({}, ViewsBindings));
PluginStore.register(new PluginManifest({}, ThreatIntelBindings));
PluginStore.register(new PluginManifest({}, AwsBindings));
PluginStore.register(new PluginManifest({}, IntegrationsBindings));
PluginStore.register(new PluginManifest({}, PerspectivesBindings));
PluginStore.register(new PluginManifest({}, NavigationBindings));

function renderAppContainer(appContainer) {
  ReactDOM.render(
    <CustomizationProvider>
      <TelemetryInit>
        <LoginQueryClientProvider>
          <GraylogThemeProvider>
            <GlobalThemeStyles />
            <AppFacade />
          </GraylogThemeProvider>
        </LoginQueryClientProvider>
      </TelemetryInit>
    </CustomizationProvider>,
    appContainer,
  );
}

const appContainer = document.querySelector('div#app-root');
renderAppContainer(appContainer);
