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
import type { TelemetryEventType } from 'logic/telemetry/TelemetryContext';

type PluginNavigationLink = {
  path: QualifiedUrl<string>;
};

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
  };
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
      password: (
        errors: { [name: string]: string },
        password: string,
        passwordRepeat: string,
      ) => { [name: string]: string };
    };
    extractSubmitError: (errors: FetchError) => string;
    onCreate: (formData: { [name: string]: string }) => { [name: string]: string };
  };
}
interface InputConfiguration {
  type: string;
  component: React.ComponentType<{}>;
  embeddedComponent?: React.ComponentType<{}>;
}
interface ProviderType {
  type: string;
  title?: string;
  formComponent: React.ComponentType<{
    onErrorChange: (error?: string) => void;
    setLoginFormState: (loginFormState: string) => void;
  }>;
}

interface LogoutHook {
  (): void | Promise<unknown>;
}

type DataTiering = {
  type: string;
  TiersConfigurationFields: React.ComponentType<{
    valuesPrefix?: string;
    hiddenFields?: string[];
    immutableFields?: string[];
    ignoreFieldRestrictions?: boolean;
  }>;
  TiersSummary: React.ComponentType<{
    config: DataTieringConfig;
  }>;
  WarmTierReadinessInfo: React.ComponentType;
  DeleteFailedSnapshotMenuItem: React.ComponentType<{
    eventKey: string;
    indexSetId: string;
  }>;
};

type InputSetupWizard = {
  EnterpriseInputSetupWizard: React.ComponentType<{
    openSteps: { [key in InputSetupWizardStep]?: StepType };
  }>;
  InputFailureLink: React.ComponentType<{
    failureType: string;
    inputId: string;
    children: React.ReactNode;
  }>;
  ExtraSetupWizardStep: React.ComponentType;
};

type License = {
  EnterpriseTrafficGraph: React.ComponentType;
  LicenseGraphWithMetrics: React.ComponentType;
  EnterpriseProductLink: React.ComponentType<{
    children: React.ReactNode;
    href: string;
    clusterId: string;
    licenseSubject?: string;
  }>;
};

export type FieldValueProvider = {
  type: string;
  displayName: string;
  formComponent: React.ComponentType<{
    fieldName: string;
    config: EventDefinition['field_spec'][number];
    onChange: (nextConfig: EventDefinition['field_spec'][number]) => void;
    validation: any;
    currentUser: User;
  }>;
  summaryComponent: React.ComponentType<{
    fieldName: string;
    keys: Array<string>;
    currentUser: User;
    config: EventDefinition['field_spec'][number];
  }>;
  defaultConfig: {
    template?: string;
    table_name?: string;
    key_field?: string;
  };
  requiredFields: string[];
};

type CreatorTelemetryEvent = {
  type: TelemetryEventType;
  section: string;
  actionValue: string;
};

type RouteGenerator = (id: string, type: string) => QualifiedUrl<string>;

type EntityTypeRouteGenerator = { type: string; route: (id: string) => QualifiedUrl<string> };

type IndexRetentionConfig = {
  type: string;
  displayName: string;
  configComponent: React.ComponentType<IndexRetentionConfigComponentProps>;
  summaryComponent: React.ComponentType<IndexRetentionSummaryComponentProps>;
};

declare module 'graylog-web-plugin/plugin' {
  type Id = string;
  type Wildcard = '*';
  type Permission =
    | Wildcard
    | {
        [Entity in keyof EntityActions]:
          | `${Entity}:${Wildcard}`
          | `${Entity}:${EntityActions[Entity]}`
          | `${Entity}:${EntityActions[Entity]}:${Id}`;
      }[keyof EntityActions];
  type Permissions = Permission | Array<Permission>;

  interface EntityCreator {
    id: string;
    title: string;
    path: QualifiedUrl<string>;
    permissions?: Permissions;
    telemetryEvent?: CreatorTelemetryEvent;
  }

  interface EntityActions {
    alerts: 'create';
    api_browser: 'read';
    authentication: 'edit';
    buffers: 'read';
    // Do we need both of the following?
    clusterconfig: 'read';
    clusterconfigentry: 'read' | 'edit';
    clusterconfiguration: 'read';
    contentpack: 'read';
    dashboards: 'create' | 'edit' | 'read';
    datanode: 'start';
    decorators: 'create' | 'edit' | 'read';
    eventdefinitions: 'create' | 'delete' | 'edit' | 'read';
    eventnotifications: 'create' | 'delete' | 'edit' | 'read';
    fieldnames: 'read';
    grok_pattern: 'read';
    indexercluster: 'read';
    indexranges: 'rebuild';
    indexset_templates: 'create' | 'edit' | 'read';
    indexsets: 'create' | 'edit' | 'read';
    indexsets_field_restrictions: 'edit';
    indices: 'read' | 'changestate' | 'failures';
    input_types: 'create';
    inputs: 'create' | 'edit' | 'read' | 'terminate' | 'changestate';
    journal: 'read';
    jvmstats: 'read';
    lbstatus: 'change';
    licenseinfos: 'read';
    licenses: 'read';
    loggers: 'read';
    loggersmessages: 'read';
    lookuptables: 'read';
    mappingprofiles: 'read';
    metrics: 'read';
    messagecount: 'read';
    messages: 'analyze' | 'read';
    node: 'shutdown';
    notifications: 'read';
    outputs: 'create' | 'edit' | 'read' | 'terminate';
    pipeline: 'create' | 'delete' | 'edit' | 'read';
    pipeline_connection: 'edit' | 'read';
    processbuffer: 'dump';
    processing: 'changestate';
    roles: 'delete' | 'edit' | 'read';
    searches: 'relative';
    sidecars: 'read';
    stream_outputs: 'create' | 'delete' | 'read';
    streams: 'create' | 'delete' | 'edit' | 'read' | 'changestate';
    system: 'read';
    systemjobs: 'read';
    systemmessages: 'read';
    team: 'edit';
    threads: 'dump';
    throughput: 'read';
    typemappings: 'edit';
    urlallowlist: 'read' | 'write';
    users:
      | 'create'
      | 'edit'
      | 'read'
      | 'tokenlist'
      | 'tokencreate'
      | 'tokenremove'
      | 'passwordchange'
      | 'rolesedit'
      | 'list';
    view: 'edit' | 'read';
  }

  interface PluginDataLake {
    StreamDataLake: React.ComponentType<{
      permissions: Immutable.List<Permission>;
    }>;
    DataLakeStatus: React.ComponentType<{
      dataLakeEnabled: boolean;
    }>;
    DataLakeJournal: React.ComponentType<{
      nodeId: string;
    }>;
    DataLakeJobs: React.ComponentType<{
      permissions: Immutable.List<Permission>;
      streamId: string;
    }>;
    StreamIlluminateProcessingSection: React.ComponentType<{
      stream: Stream;
    }>;
    StreamIndexSetDataLakeWarning: React.ComponentType<{ streamId: string; isArchivingEnabled: boolean }>;
    fetchStreamDataLakeStatus: (streamId: string) => Promise<{
      id: string;
      archive_name: string;
      enabled: boolean;
      stream_id: string;
      retention_time: number;
    }>;
    fetchStreamDataLake: (streamId: string) => Promise<{
      id: string;
      archive_config_id: string;
      message_count: number;
      archive_name: string;
      timestamp_from: string;
      timestamp_to: string;
      restore_history: Array<{ id: string }>;
    }>;
    getStreamDataLakeTableElements: (permission: Immutable.List<string>) => {
      attributeName: string;
      attributes: Array<{ id: string; title: string }>;
      columnRenderer: { data_lake: ColumnRenderer<Stream> };
    };
    DataLakeStreamDeleteWarning: React.ComponentType;
  }

  type HelpMenuItem = {
    description: string;
    permissions?: Permission | Array<Permission>;
  } & ({ externalLink?: string } | { path?: string } | { action?: (args: { showHotkeysModal: () => void }) => void });

  interface PageNavigation {
    description: string;
    perspective?: string;
    children: Array<{
      description: string;
      position?: PluginNavigation['position'];
      permissions?: Permission | Array<Permission>;
      useCondition?: () => boolean;
      requiredFeatureFlag?: string;
      path: QualifiedUrl<string>;
      exactPathMatch?: boolean;
    }>;
  }

  interface PluginRoute {
    path: string;
    component: React.ComponentType;
    parentComponent?: React.ComponentType | null;
    permissions?: Permission | Array<Permission>;
    requiredFeatureFlag?: string;
  }

  interface PluginNavigationDropdownItem {
    description: string;
    path: QualifiedUrl<string>;
    permissions?: Permission | Array<Permission>;
    requiredFeatureFlag?: string;
  }

  type PluginNavigationDropdown = {
    children: Array<PluginNavigationDropdownItem>;
  };

  type PluginNavigation = {
    description: string;
    requiredFeatureFlag?: string;
    perspective?: string;
    BadgeComponent?: React.ComponentType<{ text: string }>;
    position?: { last: true } | { after: string } | undefined;
    permissions?: Permission | Array<Permission>;
    useCondition?: () => boolean;
  } & (PluginNavigationLink | PluginNavigationDropdown);

  interface PluginExports {
    navigation?: Array<PluginNavigation>;
    /**
     * List of nav items. Define permissions if the item should only be displayed for users with specific permissions.
     * By default, an item is active if the current URL starts with the item URL.
     * If you only want to display an item as active only when its path matches exactly, set `exactPathMatch` to true.
     */
    pageNavigation?: Array<PageNavigation>;
    dataLake?: Array<PluginDataLake>;
    dataTiering?: Array<DataTiering>;
    defaultNavigation?: Array<PluginNavigation>;
    navigationItems?: Array<PluginNavigationItems>;
    globalNotifications?: Array<GlobalNotification>;
    helpMenu?: Array<HelpMenuItem>;
    fieldValueProviders?: Array<FieldValueProvider>;
    license?: Array<License>;
    inputSetupWizard?: Array<InputSetupWizard>;
    // Global context providers allow to fetch and process data once
    // and provide the result for all components in your plugin.
    globalContextProviders?: Array<React.ComponentType<React.PropsWithChildrean<{}>>>;
    // Difference between page context providers and global context providers
    // is that page context providers are rendered within the <App>.
    pageContextProviders?: Array<React.ComponentType<React.PropsWithChildrean<{}>>>;
    routes?: Array<PluginRoute>;
    pages?: PluginPages;
    pageFooter?: Array<PluginPageFooter>;
    cloud?: Array<PluginCloud>;
    forwarder?: Array<PluginForwarder>;
    inputConfiguration?: Array<InputConfiguration>;
    loginProviderType?: Array<ProviderType>;
    'hooks.logout'?: Array<LogoutHook>;
    entityRoutes?: Array<RouteGenerator>;
    entityTypeRoute?: Array<EntityTypeRouteGenerator>;
    entityCreators?: Array<EntityCreator>;
    indexRetentionConfig?: Array<IndexRetentionConfig>;
  }
  interface PluginMetadata {
    name?: string;
    author?: string;
    description?: string;
    license?: string;
  }
  interface PluginRegistration {
    metadata?: PluginMetadata;
    exports: PluginExports;
  }

  interface PluginManifest extends PluginRegistration {
    // eslint-disable-next-line @typescript-eslint/no-misused-new
    new (json: {}, exports: PluginExports): PluginManifest;
  }

  type WrapWithArray<T> = T extends Array<any> ? T : Array<T>;
  interface PluginStore {
    register: (manifest: PluginRegistration) => void;
    exports: <T extends keyof PluginExports>(key: T) => WrapWithArray<PluginExports[T]>;
    unregister: (manifest: PluginRegistration) => void;
    get: () => Array<PluginRegistration>;
  }

  const PluginStore: PluginStore;
  const PluginManifest: PluginManifest;
}
