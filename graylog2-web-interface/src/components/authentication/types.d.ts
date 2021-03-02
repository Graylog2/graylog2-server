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
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { DirectoryServiceBackendConfig } from 'logic/authentication/directoryServices/types';

interface AuthenticationService {
  name: string;
  displayName: string;
  createComponent: React.ComponentType<{}>;
  editComponent: React.ComponentType<any>;
  configDetailsComponent: React.ComponentType<any>;
  configToJson: (config: {}) => {};
  configFromJson: (json: {}) => DirectoryServiceBackendConfig;
}

interface DirectoryServicesGroupSync {
  components: {
    GroupSyncSection: React.ComponentType<{ authenticationBackend: AuthenticationBackend }>;
  }
}

interface AuthenticationPlugin {
  components: {
    SyncedTeamsSection: React.ComponentType<{ authenticationBackend: AuthenticationBackend }>;
  };
}

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    'authentication.services'?: Array<AuthenticationService>;
    'authentication.enterprise.directoryServices.groupSync'?: DirectoryServicesGroupSync;
    'authentication.enterprise'?: Array<AuthenticationPlugin>;
  }
}
