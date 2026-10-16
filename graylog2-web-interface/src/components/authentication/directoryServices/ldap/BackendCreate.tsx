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

import handleCreate from '../HandleCreate';
import BackendWizard from '../BackendWizard';
import HelpText from '../HelpText';

export const AUTH_BACKEND_META = {
  serviceType: 'ldap',
  serviceTitle: 'LDAP',
};

export const HELP = {
  // server config help
  systemUserDn: <HelpText i18nKey="ldap.help.system_user_dn" />,
  systemUserPassword: <HelpText i18nKey="ldap.help.system_user_password" />,
  // user sync help
  userSearchBase: <HelpText i18nKey="ldap.help.user_search_base" />,
  userSearchPattern: (
    <HelpText
      i18nKey="directory_help.user_search_pattern"
      values={{ example: '(&(uid={0})(objectClass=inetOrgPerson))', placeholder: '{0}' }}
    />
  ),
  userNameAttribute: <HelpText i18nKey="ldap.help.user_name_attribute" />,
  userFullNameAttribute: <HelpText i18nKey="ldap.help.user_full_name_attribute" />,
  userUniqueIdAttribute: <HelpText i18nKey="ldap.help.user_unique_id_attribute" />,
  defaultRoles: <HelpText i18nKey="directory_help.default_roles" />,
  defaultUserTimezone: <HelpText i18nKey="directory_help.default_user_timezone" />,
  emailAttributes: <HelpText i18nKey="ldap.help.email_attributes" />,
};

const INITIAL_VALUES: Partial<WizardFormValues> = {
  title: AUTH_BACKEND_META.serviceTitle,
  serverHost: 'localhost',
  serverPort: 636,
  transportSecurity: 'tls',
  userFullNameAttribute: 'cn',
  emailAttributes: ['mail', 'rfc822Mailbox'],
  userNameAttribute: 'uid',
  userUniqueIdAttribute: 'entryUUID',
  verifyCertificates: true,
};

const BackendCreate = () => {
  const { t } = useTranslation('authentication');
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const { help: groupSyncHelp = {}, initialValues: initialGroupSyncValues = {} } =
    enterpriseGroupSyncPlugin?.wizardConfig?.ldap ?? {};
  const help = { ...HELP, ...groupSyncHelp };
  const initialValues = { ...INITIAL_VALUES, ...initialGroupSyncValues };

  return (
    <DocumentTitle title={t('ldap.create_title')}>
      <WizardPageHeader />
      <BackendWizard
        onSubmit={handleCreate}
        help={help}
        authBackendMeta={AUTH_BACKEND_META}
        initialValues={initialValues}
      />
    </DocumentTitle>
  );
};

export default BackendCreate;
