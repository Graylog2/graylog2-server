// @flow strict
import * as React from 'react';

import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { DocumentTitle } from 'components/common';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';

import WizardPageHeader from './WizardPageHeader';

import type { WizardFormValues } from '../BackendWizard/BackendWizardContext';
import BackendWizard from '../BackendWizard';

export const AUTH_BACKEND_META = {
  serviceType: 'ldap',
  serviceTitle: 'LDAP',
};

export const HELP = {
  // server config help
  systemUserDn: (
    <span>
      The username for the initial connection to the LDAP server, e.g. <code>ldapbind@some.domain</code>.
      This needs to match the <code>userPrincipalName</code> of that user.
    </span>
  ),
  systemUserPassword: 'The password for the initial connection to the LDAP server.',
  // user sync help
  userSearchBase: (
    <span>
      The base tree to limit the LDAP search query to, e.g. <code>cn=users,dc=example,dc=com</code>.
    </span>
  ),
  userSearchPattern: (
    <span>
      For example <code className="text-nowrap">{'(&(objectClass=user)(sAMAccountName={0}))'}</code>.{' '}
      The string <code>{'{0}'}</code> will be replaced by the entered username.
    </span>
  ),
  userNameAttribute: (
    <span>
      Which LDAP attribute to use for the username of the user in Graylog.<br />
      Try to load a test user using the sidebar form, if you are unsure which attribute to use.
    </span>
  ),
  userFullNameAttribute: (
    <span>
      Which LDAP attribute to use for the full name of the user in Graylog, e.g. <code>displayName</code>.<br />
    </span>
  ),
  defaultRoles: (
    'The default Graylog roles determine whether a user created via LDAP can access the entire system, or has limited access.'
  ),
};

export const prepareInitialValues = () => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  let initialValues = {
    serverHost: 'localhost',
    serverPort: 636,
    transportSecurity: 'tls',
    userFullNameAttribute: 'cn',
    userNameAttribute: 'uid',
    verifyCertificates: true,
  };

  if (enterpriseGroupSyncPlugin) {
    const {
      initialValues: initialGroupSyncValues,
    } = enterpriseGroupSyncPlugin.hooks.useInitialGroupSyncValues();

    initialValues = { ...initialValues, ...initialGroupSyncValues };
  }

  return initialValues;
};

export const handleSubmit = (payload: WizardSubmitPayload, formValues: WizardFormValues, licenseIsValid?: boolean = true) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();

  return AuthenticationDomain.create(payload).then((result) => {
    if (result.backend && formValues.synchronizeGroups && enterpriseGroupSyncPlugin && licenseIsValid) {
      return enterpriseGroupSyncPlugin.actions.onDirectoryServiceBackendUpdate(false, formValues, result.backend.id, AUTH_BACKEND_META.serviceType);
    }

    return result;
  });
};

const BackendCreate = () => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const groupSyncFormHelp = enterpriseGroupSyncPlugin?.help?.ldap ?? {};
  const help = { ...HELP, ...groupSyncFormHelp };
  const initialValues = prepareInitialValues();

  return (
    <DocumentTitle title="Create LDAP Authentication Service">
      <WizardPageHeader />
      <BackendWizard onSubmit={handleSubmit}
                     help={help}
                     authBackendMeta={AUTH_BACKEND_META}
                     initialValues={initialValues} />
    </DocumentTitle>
  );
};

export default BackendCreate;
