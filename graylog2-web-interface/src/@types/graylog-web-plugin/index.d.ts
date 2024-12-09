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
import type Immutable from 'immutable';

import type FetchError from 'logic/errors/FetchError';
import type { DataTieringConfig } from 'components/indices/data-tiering';
import type { QualifiedUrl } from 'routing/Routes';
import type User from 'logic/users/User';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import type { Stream } from 'logic/streams/types';
import type { ColumnRenderer } from 'components/common/EntityDataTable/types';
import type { StepType } from 'components/common/Wizard';
import type { InputSetupWizardStep } from 'components/inputs/InputSetupWizard';

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
  path: QualifiedUrl<string>;
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
  useIsValidLicense?: () => boolean,
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

interface LogoutHook {
  (): void | Promise<unknown>;
}

type DataTiering = {
  type: string,
  TiersConfigurationFields: React.ComponentType<{valuesPrefix?: string}>,
  TiersSummary: React.ComponentType<{
    config: DataTieringConfig
  }>,
  WarmTierReadinessInfo: React.ComponentType,
  DeleteFailedSnapshotMenuItem: React.ComponentType<{
    eventKey: string,
    indexSetId: string
  }>,
}

type InputSetupWizard = {
  steps: {
    [key in InputSetupWizardStep]?: StepType
  }
}

type License = {
  EnterpriseTrafficGraph: React.ComponentType,
  LicenseGraphWithMetrics: React.ComponentType,
  EnterpriseProductLink: React.ComponentType<{
    children: React.ReactNode,
    href: string,
    clusterId: string,
    licenseSubject?: string
  }>,
}

export type FieldValueProvider = {
  type: string,
  displayName: string,
  formComponent: React.ComponentType<{
    fieldName: string,
    config: EventDefinition['field_spec'][number],
    onChange: (nextConfig: EventDefinition['field_spec'][number]) => void,
    validation: any,
    currentUser: User,
  }>,
  summaryComponent: React.ComponentType<{
    fieldName: string,
    keys: Array<string>,
    currentUser: User,
    config: EventDefinition['field_spec'][number],
  }>,
  defaultConfig: {
    template?: string,
    table_name?: string,
    key_field?: string,
  },
  requiredFields: string[],
}

interface PluginDataWarehouse {
  StreamDataWarehouse: React.ComponentType<{
    permissions: Immutable.List<string>,
  }>,
  DataWarehouseStatus: React.ComponentType<{
    datawareHouseEnabled: boolean;
  }>,
  DataWarehouseJournal: React.ComponentType<{
    nodeId: string,
  }>,
  DataWarehouseJobs: React.ComponentType<{
    permissions: Immutable.List<string>,
    streamId: string,
  }>,
  StreamIlluminateProcessingSection: React.ComponentType<{
    stream: Stream,
  }>,
  StreamIndexSetDataWarehouseWarning: React.ComponentType<{streamId: string, isArchivingEnabled: boolean}>,
  fetchStreamDataWarehouseStatus: (streamId: string) => Promise<{
    id: string,
    archive_name: string,
    enabled: boolean,
    stream_id: string,
    retention_time: number,
  }>,
  fetchStreamDataWarehouse: (streamId: string) => Promise<{
    id: string,
    archive_config_id: string,
    message_count: number,
    archive_name: string,
    timestamp_from: string,
    timestamp_to: string,
    restore_history: Array<{id:string}>,

  }>;
  getStreamDataWarehouseTableElements: (permission: Immutable.List<string>) => {
    attributeName: string,
    attributes: Array<{ id: string, title: string }>,
    columnRenderer: { datawarehouse: ColumnRenderer<Stream> },
  },
  DataWarehouseStreamDeleteWarning: React.ComponentType,
}

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    navigation?: Array<PluginNavigation>;
    dataWarehouse?: Array<PluginDataWarehouse>
    dataTiering?: Array<DataTiering>
    defaultNavigation?: Array<PluginNavigation>;
    navigationItems?: Array<PluginNavigationItems>;
    globalNotifications?: Array<GlobalNotification>;
    fieldValueProviders?:Array<FieldValueProvider>;
    license?: Array<License>,
    inputSetupWizard?: Array<InputSetupWizard>;
    // Global context providers allow to fetch and process data once
    // and provide the result for all components in your plugin.
    globalContextProviders?: Array<React.ComponentType<React.PropsWithChildrean<{}>>>,
    // Difference between page context providers and global context providers
    // is that page context providers are rendered within the <App> giving it
    // access to certain contexts like PerspectivesContext
    pageContextProviders?: Array<React.ComponentType<React.PropsWithChildrean<{}>>>,
    routes?: Array<PluginRoute>;
    entityRoutes?: Array<(id: string, type: string) => string>
    pages?: PluginPages;
    pageFooter?: Array<PluginPageFooter>;
    cloud?: Array<PluginCloud>;
    forwarder?: Array<PluginForwarder>;
    inputConfiguration?: Array<InputConfiguration>;
    loginProviderType?: Array<ProviderType>;
    'hooks.logout'?: Array<LogoutHook>,
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
