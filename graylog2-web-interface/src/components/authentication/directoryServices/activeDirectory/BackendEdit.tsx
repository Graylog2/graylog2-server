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
import * as React from 'react';

import { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import { DocumentTitle, Spinner } from 'components/common';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';

import WizardPageHeader from './WizardPageHeader';
import { HELP, AUTH_BACKEND_META } from './BackendCreate';

import prepareInitialWizardValues from '../PrepareInitialWizardValues';
import BackendWizard from '../BackendWizard';
import handleUpdate from '../HandleUpdate';

type Props = {
  authenticationBackend: DirectoryServiceBackend,
  initialStepKey: string | null | undefined,
};

const _optionalWizardProps = (initialStepKey: string | null | undefined) => {
  return { initialStepKey }
};

const BackendEdit = ({ authenticationBackend, initialStepKey }: Props) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const {
    help: groupSyncHelp = {},
    excludedFields: groupSyncExcludedFields = {},
    initialValues: initialGroupSyncValues = {},
  } = enterpriseGroupSyncPlugin?.wizardConfig?.activeDirectory ?? {};
  const help = { ...HELP, ...groupSyncHelp };
  const excludedFields = { ...groupSyncExcludedFields, userUniqueIdAttribute: true };
  let initialValues = prepareInitialWizardValues(authenticationBackend);

  if (enterpriseGroupSyncPlugin) {
    const {
      formValues: groupFormValues,
      finishedLoading,
    } = enterpriseGroupSyncPlugin.hooks.useInitialGroupSyncValues(authenticationBackend.id, initialGroupSyncValues);

    if (!finishedLoading) {
      return <Spinner />;
    }

    initialValues = { ...initialValues, ...groupFormValues };
  }

  const authBackendMeta = {
    ...AUTH_BACKEND_META,
    backendId: authenticationBackend.id,
    backendHasPassword: authenticationBackend.config.systemUserPassword.isSet,
    backendGroupSyncIsActive: !!initialValues.synchronizeGroups,
  };
  const _handleSubmit = (
    payload,
    formValues,
    serviceType,
    shouldUpdateGroupSync,
  ) => handleUpdate(
    payload,
    formValues,
    authenticationBackend.id,
    !!initialValues.synchronizeGroups,
    serviceType,
    shouldUpdateGroupSync,
  );

  return (
    <DocumentTitle title="Edit Active Directory Authentication Service">
      <WizardPageHeader authenticationBackend={authenticationBackend} />
      <BackendWizard {..._optionalWizardProps(initialStepKey)}
                     authBackendMeta={authBackendMeta}
                     excludedFields={excludedFields}
                     help={help}
                     initialValues={initialValues}
                     onSubmit={_handleSubmit} />
    </DocumentTitle>
  );
};

export default BackendEdit;
