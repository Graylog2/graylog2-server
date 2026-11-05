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
import { useTranslation } from 'react-i18next';

import { DocumentTitle } from 'components/common';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import type { WizardFormValues } from 'components/authentication/directoryServices/BackendWizard/BackendWizardContext';

import WizardPageHeader from './WizardPageHeader';

import BackendWizard from '../BackendWizard';
import HelpText from '../HelpText';
import handleCreate from '../HandleCreate';

export const HELP = {
  // server config help
  systemUserDn: <HelpText i18nKey="active_directory.help.system_user_dn" />,
  systemUserPassword: <HelpText i18nKey="active_directory.help.system_user_password" />,
  // user sync help
  userSearchBase: <HelpText i18nKey="active_directory.help.user_search_base" />,
  userSearchPattern: (
    <HelpText
      i18nKey="directory_help.user_search_pattern"
      values={{ example: '(&(objectClass=user)(|(sAMAccountName={0})(userPrincipalName={0})))', placeholder: '{0}' }}
    />
  ),
  userNameAttribute: <HelpText i18nKey="active_directory.help.user_name_attribute" />,
  userFullNameAttribute: <HelpText i18nKey="active_directory.help.user_full_name_attribute" />,
  defaultRoles: <HelpText i18nKey="directory_help.default_roles" />,
  defaultUserTimezone: <HelpText i18nKey="directory_help.default_user_timezone" />,
};

export const AUTH_BACKEND_META = {
  serviceTitle: 'Active Directory',
  serviceType: 'active-directory',
};

const INITIAL_VALUES: Partial<WizardFormValues> = {
  title: AUTH_BACKEND_META.serviceTitle,
  serverHost: 'localhost',
  serverPort: 636,
  transportSecurity: 'tls',
  userSearchPattern: '(&(objectClass=user)(|(sAMAccountName={0})(userPrincipalName={0})))',
  userFullNameAttribute: 'displayName',
  userNameAttribute: 'userPrincipalName',
  verifyCertificates: true,
};

const BackendCreate = () => {
  const { t } = useTranslation('authentication');
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const {
    help: groupSyncHelp = {},
    excludedFields: groupSyncExcludedFields = {},
    initialValues: initialGroupSyncValues,
  } = enterpriseGroupSyncPlugin?.wizardConfig?.activeDirectory ?? {};
  const help = { ...HELP, ...groupSyncHelp };
  const initialValues = { ...INITIAL_VALUES, ...initialGroupSyncValues };
  const excludedFields = { ...groupSyncExcludedFields, userUniqueIdAttribute: true, emailAttributes: true };

  return (
    <DocumentTitle title={t('active_directory.create_title')}>
      <WizardPageHeader />
      <BackendWizard
        authBackendMeta={AUTH_BACKEND_META}
        help={help}
        excludedFields={excludedFields}
        initialValues={initialValues}
        onSubmit={handleCreate}
      />
    </DocumentTitle>
  );
};

export default BackendCreate;
