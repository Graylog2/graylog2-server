// @flow strict
import * as React from 'react';

import { DocumentTitle, PageHeader } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendCreateGettingStarted from 'components/authentication/BackendCreateGettingStarted';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';

const AuthenticationCreatePage = () => {
  return (
    <DocumentTitle title="Create Authentication Service">
      <PageHeader title="Create Authentication Service">
        <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
        <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                 text="documentation" />.
        </span>
        <BackendOverviewLinks />
      </PageHeader>

      <BackendCreateGettingStarted />
    </DocumentTitle>
  );
};

export default AuthenticationCreatePage;
