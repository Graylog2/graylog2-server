// @flow strict
import * as React from 'react';

import {} from 'components/authentication'; // Bind all authentication plugins
import { DocumentTitle, PageHeader } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendsOverview from 'components/authentication/BackendsOverview';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import { useActiveBackend } from 'components/authentication/hooks';

const AuthenticationCreatePage = () => {
  const { finishedLoading, activeBackend } = useActiveBackend();

  return (
    <DocumentTitle title="Authentication Services">
      <PageHeader title="Authentication Services">
        <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
        <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                 text="documentation" />.
        </span>
        <BackendOverviewLinks activeBackend={activeBackend}
                              finishedLoading={finishedLoading} />
      </PageHeader>

      <BackendsOverview />
    </DocumentTitle>
  );
};

export default AuthenticationCreatePage;
