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

import * as React from 'react';
import { createRoot } from 'react-dom/client';
import Reflux from 'reflux';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import AppFacade from 'routing/AppFacade';
import ViewsBindings from 'views/bindings';
import ThreatIntelBindings from 'threatintel/bindings';
import AwsBindings from 'aws/bindings';
import IntegrationsBindings from 'integrations/bindings';
import EventDefinitionsBindings from 'components/event-definitions/event-definition-types/bindings';
import EventNotificationsBindings from 'components/event-notifications/event-notification-types/bindings';
import FieldValueProvidersBindings from 'components/event-definitions/event-definition-form/field-value-providers/bindings';
import CancellablePromise from 'logic/rest/CancellablePromise';
import TelemetryInit from 'logic/telemetry/TelemetryInit';
import LoginQueryClientProvider from 'contexts/LoginQueryClientProvider';
import PerspectivesBindings from 'components/perspectives/bindings';
import NavigationBindings from 'components/navigation/bindings';
import SecurityBindings from 'components/security/bindings';
import EventsBindings from 'components/events/bindings';
import ComponentsBindings from 'components/common/bindings';
import EntityCreatorBindings from 'components/entityCreatorBindings';
import UsersBindings from 'components/users/bindings';
import SidecarsBindings from 'components/sidecars/bindings';
import PipelinesBindings from 'components/pipelines/bindings';
import IndicesBindings from 'components/indices/bindings';
import LookupTablesBindings from 'components/lookup-tables/bindings';
import AuthenticationBindings from 'components/authentication/bindings';
import ClusterConfigurationBindings from 'components/cluster-configuration/bindings';
import ConfigurationBindings from 'components/configurations/bindings';

import '@graylog/sawmill/fonts';
import '@mantine/core/styles.css';
import '@mantine/dropzone/styles.css';
import '@mantine/notifications/styles.css';

Reflux.setPromiseFactory((handlers) => CancellablePromise.of(new Promise(handlers)));

PluginStore.register(new PluginManifest({}, ViewsBindings));
PluginStore.register(new PluginManifest({}, ThreatIntelBindings));
PluginStore.register(new PluginManifest({}, AwsBindings));
PluginStore.register(new PluginManifest({}, IntegrationsBindings));
PluginStore.register(new PluginManifest({}, EventDefinitionsBindings));
PluginStore.register(new PluginManifest({}, EventNotificationsBindings));
PluginStore.register(new PluginManifest({}, FieldValueProvidersBindings));
PluginStore.register(new PluginManifest({}, PerspectivesBindings));
PluginStore.register(new PluginManifest({}, NavigationBindings));
PluginStore.register(new PluginManifest({}, SecurityBindings));
PluginStore.register(new PluginManifest({}, EventsBindings));
PluginStore.register(new PluginManifest({}, UsersBindings));
PluginStore.register(new PluginManifest({}, SidecarsBindings));
PluginStore.register(new PluginManifest({}, ComponentsBindings));
PluginStore.register(new PluginManifest({}, EntityCreatorBindings));
PluginStore.register(new PluginManifest({}, PipelinesBindings));
PluginStore.register(new PluginManifest({}, IndicesBindings));
PluginStore.register(new PluginManifest({}, LookupTablesBindings));
PluginStore.register(new PluginManifest({}, AuthenticationBindings));
PluginStore.register(new PluginManifest({}, ClusterConfigurationBindings));
PluginStore.register(new PluginManifest({}, ConfigurationBindings));

const appContainer = document.querySelector('div#app-root');
const root = createRoot(appContainer);

root.render(
  <TelemetryInit>
    <LoginQueryClientProvider>
      <AppFacade />
    </LoginQueryClientProvider>
  </TelemetryInit>,
);
