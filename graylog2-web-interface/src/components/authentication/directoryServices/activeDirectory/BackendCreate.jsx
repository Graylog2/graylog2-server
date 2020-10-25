// @flow strict
import * as React from 'react';

import { DocumentTitle } from 'components/common';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';

import WizardPageHeader from './WizardPageHeader';

import BackendWizard from '../BackendWizard';
import { handleSubmit } from '../ldap/BackendCreate';

export const HELP = {
  // server config help
  systemUserDn: (
    <span>
      The username for the initial connection to the Active Directory server, e.g. <code>ldapbind@some.domain</code>.<br />
      This needs to match the <code>userPrincipalName</code> of that user.
    </span>
  ),
  systemUserPassword: 'The password for the initial connection to the Active Directory server.',
  // user sync help
  userSearchBase: (
    <span>
      The base tree to limit the Active Directory search query to, e.g. <code>cn=users,dc=example,dc=com</code>.
    </span>
  ),
  userSearchPattern: (
    <span>
      For example <code className="text-nowrap">{'(&(objectClass=user)(|(sAMAccountName={0})(userPrincipalName={0})))'}</code>.{' '}
      The string <code>{'{0}'}</code> will be replaced by the entered username.
    </span>
  ),
  userNameAttribute: (
    <span>
      Which Active Directory attribute to use for the full name of the user in Graylog, e.g. <code>userPrincipalName</code>.<br />
      Try to load a test user in the sidebar section <i>User Login Test</i>, if you are unsure which attribute to use.
    </span>
  ),
  userFullNameAttribute: (
    <span>
      Which Active Directory attribute to use for the full name of a synchronized Graylog user, e.g. <code>displayName</code>.<br />
    </span>
  ),
  defaultRoles: (
    <span>The default Graylog roles synchronized user will obtain. All users need the <code>Reader</code> role, to use the Graylog web interface</span>
  ),
};

const INITIAL_VALUES = {
  serverHost: 'localhost',
  serverPort: 636,
  transportSecurity: 'tls',
  userSearchPattern: '(&(objectClass=user)(|(sAMAccountName={0})(userPrincipalName={0})))',
  userFullNameAttribute: 'displayName',
  userNameAttribute: 'userPrincipalName',
  verifyCertificates: true,
};

export const AUTH_BACKEND_META = {
  serviceTitle: 'Active Directory',
  serviceType: 'active-directory',
};

const BackendCreate = () => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const {
    help: groupSyncHelp = {},
    excludedFields: groupSyncExcludedFields = {},
    initialValues: initialGroupSyncValues,
  } = enterpriseGroupSyncPlugin?.wizardConfig?.activeDirectory ?? {};
  const help = { ...HELP, ...groupSyncHelp };
  const initialValues = { ...INITIAL_VALUES, ...initialGroupSyncValues };
  const excludedFields = { ...groupSyncExcludedFields, userUniqueIdAttribute: true };

  return (
    <DocumentTitle title="Create Active Directory Authentication Services">
      <WizardPageHeader />
      <BackendWizard authBackendMeta={AUTH_BACKEND_META}
                     help={help}
                     excludedFields={excludedFields}
                     initialValues={initialValues}
                     onSubmit={handleSubmit} />
    </DocumentTitle>
  );
};

export default BackendCreate;
