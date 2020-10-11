// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import type { LdapCreate } from 'logic/authentication/ldap/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { DocumentTitle } from 'components/common';

import WizardPageHeader from './WizardPageHeader';

import type { WizardFormValues } from '../BackendWizard/contexts/BackendWizardContext';
import BackendWizard from '../BackendWizard';

export const AUTH_BACKEND_META = {
  serviceType: 'ldap',
  serviceTitle: 'LDAP',
  urlScheme: 'ldap',
};

export const prepareInitialValues = () => {
  const authGroupSyncPlugins = PluginStore.exports('authentication.enterprise.ldap.groupSync');
  let initialValues = {
    serverUrlHost: 'localhost',
    serverUrlPort: 389,
    transportSecurity: 'tls',
    userFullNameAttribute: 'cn',
    userNameAttribute: 'uid',
    verifyCertificates: true,
  };

  if (typeof authGroupSyncPlugins?.[0]?.hooks?.useInitialGroupSyncValues === 'function') {
    const {
      initialValues: initialGroupSyncValues,
    } = authGroupSyncPlugins[0].hooks.useInitialGroupSyncValues();

    initialValues = { ...initialValues, ...initialGroupSyncValues };
  }

  return initialValues;
};

export const handleSubmit = (payload: LdapCreate, formValues: WizardFormValues) => {
  const authGroupSyncPlugins = PluginStore.exports('authentication.enterprise.ldap.groupSync');

  return AuthenticationDomain.create(payload).then((result) => {
    if (result && formValues.synchronizeGroups && typeof authGroupSyncPlugins?.[0]?.actions?.handleUpdate === 'function') {
      return authGroupSyncPlugins[0].actions.handleUpdate.handleUpdate(formValues, result.backend.id, AUTH_BACKEND_META.serviceType);
    }

    return result;
  });
};

const BackendCreate = () => {
  const initialValues = prepareInitialValues();

  return (
    <DocumentTitle title="Create LDAP Authentication Service">
      <WizardPageHeader />
      <BackendWizard onSubmit={handleSubmit}
                     authBackendMeta={AUTH_BACKEND_META}
                     initialValues={initialValues} />
    </DocumentTitle>
  );
};

export default BackendCreate;
