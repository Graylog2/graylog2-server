// @flow strict
import React from 'react';

import AuthenticationOverviewLinks from 'components/authentication/AuthenticationOverviewLinks';
import AuthenticatorActionLinks from 'components/authentication/AuthenticatorActionLinks';
import AuthenticatorsDetails from 'components/authentication/AuthenticatorsDetails';
import { PageHeader, DocumentTitle } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

const AuthenticatorsPage = () => (
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

      <AuthenticationOverviewLinks />

    </PageHeader>

    <AuthenticatorsDetails />
  </DocumentTitle>
);

export default AuthenticatorsPage;
