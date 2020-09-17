// @flow strict
import * as React from 'react';

import { DocumentTitle } from 'components/common';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';

import WizardPageHeader from './WizardPageHeader';

import BackendWizard from '../BackendWizard';

const BackendCreate = () => (
  <DocumentTitle title="Create LDAP Authentication Service">
    <WizardPageHeader />
    <BackendWizard onSubmit={AuthenticationDomain.create}
                   authServiceType="ldap" />
  </DocumentTitle>
);

export default BackendCreate;
