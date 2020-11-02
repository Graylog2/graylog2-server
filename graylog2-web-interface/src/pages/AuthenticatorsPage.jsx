// @flow strict
import React from 'react';

import AuthenticatorActionLinks from 'components/authentication/AuthenticatorActionLinks';
import AuthenticatorsDetails from 'components/authentication/AuthenticatorsDetails';
import { PageHeader, DocumentTitle } from 'components/common';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import useActiveBackend from 'components/authentication/useActiveBackend';

const AuthenticatorsPage = () => {
  const { finishedLoading, activeBackend } = useActiveBackend();

  return (
    <DocumentTitle title="Authenticators Details">
      <PageHeader title="Authenticators Details" subactions={<AuthenticatorActionLinks />}>
        <span>
          Configure the single sign-on authenticator.
        </span>

        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.AUTHENTICATORS}
                             text="documentation" />
        </span>

        <BackendOverviewLinks activeBackend={activeBackend}
                              finishedLoading={finishedLoading} />

      </PageHeader>

      <AuthenticatorsDetails />
    </DocumentTitle>
  );
};

export default AuthenticatorsPage;
