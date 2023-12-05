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
import type * as React from 'react';

import type FetchError from 'logic/errors/FetchError';

interface PluginRoute {
  path: string;
  component: React.ComponentType;
  parentComponent?: React.ComponentType | null;
  permissions?: string | Array<string>;
  requiredFeatureFlag?: string;
}

interface PluginNavigationDropdownItem {
  description: string,
  path: string,
  permissions?: string | Array<string>,
  requiredFeatureFlag?: string,
}

type PluginNavigationLink = {
  path: string;
}

type PluginNavigationDropdown = {
  children: Array<PluginNavigationDropdownItem>;
}

type PluginNavigation = {
  description: string;
  requiredFeatureFlag?: string;
  perspective?: string;
  BadgeComponent?: React.ComponentType<{ text: string }>;
  position?: 'last' | undefined,
} & (PluginNavigationLink | PluginNavigationDropdown)

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

interface ForwarderInput {
  id: string;
  title: string;
}

interface Forwarder {
  title: string;
  node_id: string;
}
interface PluginForwarder {
  ForwarderReceivedBy: React.ComponentType<{
    inputId: string;
    forwarderNodeId: string;
  }>;
  fetchForwarderInput: (inputId: string) => Promise<ForwarderInput>;
  fetchForwarderNode: (nodeId: string) => Promise<Forwarder>;
  isLocalNode: (nodeId: string) => Promise<boolean>;
  messageLoaders: {
    ForwarderInputDropdown: React.ComponentType<{
      autoLoadMessage?: boolean;
      preselectedInputId?: string;
      title?: string;
      label?: string;
      loadButtonDisabled?: boolean;
      onLoadMessage: (selectedInput: string) => void;
    }>;
  };
}

interface PluginCloud {
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
  embeddedComponent?: React.ComponentType<{}>;
}
interface ProviderType {
  type: string;
  formComponent: React.ComponentType<{
    onErrorChange: (error?: string) => void;
    setLoginFormState: (loginFormState: string) => void;
  }>;
}

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    navigation?: Array<PluginNavigation>;
    defaultNavigation?: Array<PluginNavigation>;
    navigationItems?: Array<PluginNavigationItems>;
    globalNotifications?: Array<GlobalNotification>
    // Global context providers allow to fetch and process data once
    // and provide the result for all components in your plugin.
    globalContextProviders?: Array<React.ComponentType<React.PropsWithChildrean<{}>>>,
    routes?: Array<PluginRoute>;
    entityRoutes?: Array<(id: string, type: string) => string>
    pages?: PluginPages;
    pageFooter?: Array<PluginPageFooter>;
    cloud?: Array<PluginCloud>;
    forwarder?: Array<PluginForwarder>;
    inputConfiguration?: Array<InputConfiguration>;
    loginProviderType?: Array<ProviderType>;
  }
  interface PluginMetadata {
    name?: string,
    author?: string,
    description?: string,
    license?: string,
  }
  interface PluginRegistration {
    metadata?: PluginMetadata
    exports: PluginExports;
  }

  interface PluginManifest extends PluginRegistration {
    new (json: {}, exports: PluginExports): PluginManifest;
  }

  interface PluginStore {
    register: (manifest: PluginRegistration) => void;
    exports: <T extends keyof PluginExports>(key: T) => PluginExports[T];
    unregister: (manifest: PluginRegistration) => void;
    get: () => Array<PluginRegistration>
  }

  const PluginStore: PluginStore;
  const PluginManifest: PluginManifest;
}
