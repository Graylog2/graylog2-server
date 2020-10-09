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

const BackendCreate = () => {
  const authGroupSyncPlugins = PluginStore.exports('authentication.enterprise.ldap.groupSync');
  const groupSyncActions = authGroupSyncPlugins?.[0]?.actions;

  const _handleSubmit = (payload: LdapCreate, formValues: WizardFormValues) => {
    return AuthenticationDomain.create(payload).then((result) => {
      if (result && formValues.synchronizeGroups && groupSyncActions?.handleUpdate) {
        return groupSyncActions.handleUpdate(formValues, result.backend.id, AUTH_BACKEND_META.serviceType);
      }

      return result;
    });
  };

  return (
    <DocumentTitle title="Create LDAP Authentication Service">
      <WizardPageHeader />
      <BackendWizard onSubmit={_handleSubmit}
                     authBackendMeta={AUTH_BACKEND_META} />
    </DocumentTitle>
  );
};

export default BackendCreate;
