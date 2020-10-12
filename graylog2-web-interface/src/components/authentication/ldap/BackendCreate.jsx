// @flow strict
import * as React from 'react';

import type { WizardSubmitPayload } from 'logic/authentication/ldap/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { DocumentTitle } from 'components/common';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';

import WizardPageHeader from './WizardPageHeader';

import type { WizardFormValues } from '../BackendWizard/contexts/BackendWizardContext';
import BackendWizard from '../BackendWizard';

export const AUTH_BACKEND_META = {
  serviceType: 'ldap',
  serviceTitle: 'LDAP',
  urlScheme: 'ldap',
};

export const prepareInitialValues = () => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  let initialValues = {
    serverUrlHost: 'localhost',
    serverUrlPort: 636,
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

export const handleSubmit = (payload: WizardSubmitPayload, formValues: WizardFormValues) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();

  return AuthenticationDomain.create(payload).then((result) => {
    if (result && formValues.synchronizeGroups && enterpriseGroupSyncPlugin) {
      return enterpriseGroupSyncPlugin.actions.handleUpdate.handleBackendUpdate(false, formValues, result.backend.id, AUTH_BACKEND_META.serviceType);
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
