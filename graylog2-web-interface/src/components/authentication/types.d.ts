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

import type AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import type {
  DirectoryServiceBackend,
  DirectoryServiceBackendConfig,
  DirectoryServiceBackendConfigJson,
  WizardSubmitPayload,
} from 'logic/authentication/directoryServices/types';
import type { OktaBackend, OktaBackendConfig, OktaBackendConfigJson } from 'logic/authentication/okta/types';
import type Role from 'logic/roles/Role';
import type { WizardFormValues } from 'components/authentication/directoryServices/BackendWizard/BackendWizardContext';

export interface DirectoryServiceAuthenticationService {
  name: string;
  displayName: string;
  createComponent: React.ComponentType<{}>;
  editComponent: React.ComponentType<{
    authenticationBackend: (typeof DirectoryServiceBackend | typeof OktaBackendConfig),
    initialStepKey: string | null | undefined
  }>;
  configDetailsComponent: React.ComponentType<{
    authenticationBackend:(typeof AuthenticationBackend | typeof OktaBackend),
    roles?: Immutable.List<Role>,
  }>;
  configToJson: (config: {}) => DirectoryServiceBackendConfigJson;
  configFromJson: (json: {}) => DirectoryServiceBackendConfig;
}

interface OktaAuthenticationService {
  name: 'okta';
  displayName: string;
  createComponent: React.ComponentType;
  editComponent: React.ComponentType;
  configDetailsComponent: React.ComponentType;
  configToJson: (config: {}) => OktaBackendConfigJson;
  configFromJson: (json: {}) => OktaBackendConfig;
}

interface GroupSyncSectionProps {
  authenticationBackend: DirectoryServiceBackend;
  excludedFields: { [field: string]: boolean };
  roles: Immutable.List<Role>;
}

interface MatchingGroupsProviderProps {
  prepareSubmitPayload: (overrideFormValues: WizardFormValues) => WizardSubmitPayload;
}

interface GroupSyncFormProps {
  formRef: React.Ref<FormikProps<WizardFormValues>>;
  help: { [inputName: string]: React.ReactElement | string | null | undefined };
  excludedFields: { [name: string]: boolean };
  onSubmitAll: (shouldUpdate?: boolean) => Promise<void>;
  prepareSubmitPayload: (formValues: WizardFormValues) => WizardSubmitPayload;
  roles: Immutable.List<Role>;
  submitAllError: React.ReactNode;
  validateOnMount: boolean;
}
type SupportedBackends = 'ldap' | 'activeDirectory';

interface Backend {
  help: { [inputName: string]: React.ReactElement | string | null | undefined };
  initialValues: WizardFormValues;
  excludedFields: { [field: string]: boolean };
}

interface DirectoryServicesGroupSync {
  actions: {
    onDirectoryServiceBackendUpdate: (backendGroupSyncIsActive: boolean, formValues: WizardFormValues, backendId: string, serviceType: string) => Promise<void>;
  },
  validation: {
    GroupSyncValidation: (teamType: string) => {};
  },
  components: {
    GroupSyncSection: React.ComponentType<GroupSyncSectionProps>;
    MatchingGroupsProvider: React.ComponentType<MatchingGroupsProviderProps>;
    GroupSyncForm: React.ComponentType<GroupSyncFormProps>;
  },
  wizardConfig: Record<SupportedBackends, Backend>,
  hooks: {
    useInitialGroupSyncValues: (backendId: string, formValues: WizardFormValues) => { formValues: WizardFormValues, finishedLoading: boolean };
  }
}

interface AuthenticationPlugin {
  components: {
    SyncedTeamsSection: React.ComponentType<{ authenticationBackend: AuthenticationBackend }>;
  };
}

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    'authentication.services'?: Array<DirectoryServiceAuthenticationService | OktaAuthenticationService>;
    'authentication.enterprise.directoryServices.groupSync'?: Array<DirectoryServicesGroupSync>;
    'authentication.enterprise'?: Array<AuthenticationPlugin>;
  }
}
