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
import FetchError from 'logic/errors/FetchError';

interface PluginRoute {
  path: string;
  component: React.ComponentType;
  parentComponent?: React.ComponentType | null;
  permissions?: string;
}
interface PluginNavigation {
  path: string;
  description: string;
}
interface PluginNavigationItems {
  key: string;
  component: React.ComponentType<{ smallScreen?: boolean }>;
}
interface GlobalNotification {
  key: string;
  component: React.ComponentType;
}

interface PluginPages {
  search?: {
    component: React.ComponentType;
  }
}

interface PluginPageFooter {
  component: React.ComponentType;
}

interface PluginCloud {
  ForwarderReceivedBy: React.ComponentType<{
    inputId: string;
    forwarderNodeId: string;
  }>;
  isLocalNode: (nodeId: string) => NodeInfo;
  messageLoaders: {
    ForwarderInputDropdown: React.ComponentType<{
      autoLoadMessage?: boolean;
      preselectedInputId?: string;
      title?: string;
      loadButtonDisabled?: boolean;
      onLoadMessage: (selectedInput: string) => void;
    }>;
  };
  oktaUserForm: {
    fields: {
      username: React.ComponentType<{}> | null;
      email: React.ComponentType<{}>;
      password: React.ComponentType<{}>;
    };
    validations: {
      password: (errors: { [name: string]: string }, password: string, passwordRepeat: string) => { [name: string]: string };
    };
    extractSubmitError: (errors: FetchError) => string;
    onCreate: (formData: { [name: string ]: string }) => { [name: string]: string };
  };
}
interface InputConfiguration {
  type: string;
  component: React.ComponentType<{}>;
  embeddedComponent: React.ComponentType<{}> | null;
}
declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    navigation?: Array<PluginNavigation>;
    navigationItems?: Array<PluginNavigationItems>;
    globalNotifications?: Array<GlobalNotification>
    routes?: Array<PluginRoute>;
    pages?: PluginPages;
    pageFooter?: Array<PluginPageFooter>;
    cloud?: Array<PluginCloud>;
    inputConfiguration?: Array<InputConfiguration>
  }

  interface PluginRegistration {
    exports: PluginExports;
  }

  interface PluginManifest extends PluginRegistration {
    new (json: {}, exports: PluginExports): PluginManifest;
  }

  interface PluginStore {
    register: (manifest: PluginRegistration) => void;
    exports: <T extends keyof PluginExports>(key: T) => PluginExports[T];
    unregister: (manifest: PluginRegistration) => void;
  }

  const PluginStore: PluginStore;
  const PluginManifest: PluginManifest;
}
