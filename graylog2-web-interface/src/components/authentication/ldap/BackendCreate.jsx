// @flow strict
import * as React from 'react';

import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { DocumentTitle } from 'components/common';

import WizardPageHeader from './WizardPageHeader';

import BackendWizard from '../BackendWizard';

export const AUTH_BACKEND_META = {
  serviceType: 'ldap',
  serviceTitle: 'LDAP',
  urlScheme: 'ldap',
};

const BackendCreate = () => (
  <DocumentTitle title="Create LDAP Authentication Service">
    <WizardPageHeader />
    <BackendWizard onSubmit={AuthenticationDomain.create}
                   authBackendMeta={AUTH_BACKEND_META} />
  </DocumentTitle>
);

export default BackendCreate;
