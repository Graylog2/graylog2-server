// @flow strict
import * as React from 'react';

import DocsHelper from 'util/DocsHelper';
import { PageHeader, DocumentTitle } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

import BackendWizard from '../BackendWizard';

const BackendCreate = () => {
  return (
    <DocumentTitle title="Create LDAP Authentication Provider">
      <PageHeader title="Create LDAP Authentication Provider">
        <span>Configure Graylog&apos;s authentication providers of this Graylog cluster.</span>
        <span>
          Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                             text="documentation" />.
        </span>
      </PageHeader>
      <BackendWizard />
    </DocumentTitle>
  );
};

export default BackendCreate;
