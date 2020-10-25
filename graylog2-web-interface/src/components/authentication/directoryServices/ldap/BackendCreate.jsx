// @flow strict
import * as React from 'react';

import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { DocumentTitle } from 'components/common';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';

import WizardPageHeader from './WizardPageHeader';

import type { WizardFormValues, AuthBackendMeta } from '../BackendWizard/BackendWizardContext';
import BackendWizard from '../BackendWizard';

export const AUTH_BACKEND_META = {
  serviceType: 'ldap',
  serviceTitle: 'LDAP',
};

export const HELP = {
  // server config help
  systemUserDn: (
    <span>
      The username for the initial connection to the LDAP server, e.g. <code>cn=admin,dc=example,dc=com</code>,
      this might be optional depending on your LDAP server.
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
      For example <code className="text-nowrap">{'(&(uid={0})(objectClass=inetOrgPerson))'}</code>.{' '}
      The string <code>{'{0}'}</code> will be replaced by the entered username.
    </span>
  ),
  userNameAttribute: (
    <span>
      Which LDAP attribute to use for the username of the user in Graylog, e.g <code>uid</code>.<br />
      Try to load a test user in the sidebar section <i>User Login Test</i>, if you are unsure which attribute to use.
    </span>
  ),
  userFullNameAttribute: (
    <span>
      Which LDAP attribute to use for the full name of a synchronized Graylog user, e.g. <code>cn</code>.<br />
    </span>
  ),
  userUniqueIdAttribute: (
    <span>
      Which LDAP attribute to use for the ID of a synchronized Graylog user, e.g. <code>entryUUID</code>.<br />
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
  userFullNameAttribute: 'cn',
  userNameAttribute: 'uid',
  userUniqueIdAttribute: 'entryUUID',
  verifyCertificates: true,
};

export const handleSubmit = (payload: WizardSubmitPayload, formValues: WizardFormValues, serviceType: $PropertyType<AuthBackendMeta, 'serviceType'>, licenseIsValid?: boolean = true) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const shouldCreateGroupSync = formValues.synchronizeGroups && enterpriseGroupSyncPlugin && licenseIsValid;
  const backendCreateNotificationSettings = {
    notifyOnSuccess: !shouldCreateGroupSync,
  };

  return AuthenticationDomain.create(backendCreateNotificationSettings)(payload).then((result) => {
    if (result.backend && formValues.synchronizeGroups && enterpriseGroupSyncPlugin && licenseIsValid) {
      return enterpriseGroupSyncPlugin.actions.onDirectoryServiceBackendUpdate(false, formValues, result.backend.id, AUTH_BACKEND_META.serviceType);
    }

    return result;
  });
};

const BackendCreate = () => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const {
    help: groupSyncHelp = {},
    initialValues: initialGroupSyncValues = {},
  } = enterpriseGroupSyncPlugin?.wizardConfig?.ldap ?? {};
  const help = { ...HELP, ...groupSyncHelp };
  const initialValues = { ...INITIAL_VALUES, ...initialGroupSyncValues };

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
