// @flow strict
import * as React from 'react';

import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import DocsHelper from 'util/DocsHelper';
import { PageHeader, DocumentTitle } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';

import BackendWizard from '../BackendWizard';

const BackendCreate = () => {
  return (
    <DocumentTitle title="Create LDAP Authentication Service">
      <PageHeader title="Create LDAP Authentication Service">
        <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
        <span>
          Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                             text="documentation" />.
        </span>
        <BackendOverviewLinks />
      </PageHeader>
      <BackendWizard onSubmit={AuthenticationDomain.create} authServiceType="ldap" />
    </DocumentTitle>
  );
};

export default BackendCreate;
