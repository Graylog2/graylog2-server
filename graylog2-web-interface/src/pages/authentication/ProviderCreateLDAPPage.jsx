// @flow strict
import * as React from 'react';

import ProviderCreateLDAP from 'components/authentication/ProviderCreateLDAP';
import DocsHelper from 'util/DocsHelper';
import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';

const ProviderCreateLDAPPage = () => (
  <>
    <PageHeader title="Create LDAP Authentication Provider">
      <span>Configure Graylog&apos;s authentication providers of this Graylog cluster.</span>
      <span>
        Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                           text="documentation" />.
      </span>
    </PageHeader>

    <ProviderCreateLDAP />

  </>
);

export default ProviderCreateLDAPPage;
