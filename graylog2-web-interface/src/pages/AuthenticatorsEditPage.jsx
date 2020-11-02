// @flow strict
import React from 'react';

import AuthenticationOverviewLinks from 'components/authentication/AuthenticationOverviewLinks';
import AuthenticatorActionLinks from 'components/authentication/AuthenticatorActionLinks';
import AuthenticatorsEdit from 'components/authentication/AuthenticatorsEdit';
import { PageHeader, DocumentTitle } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

const AuthenticatorsEditPage = () => (
  <DocumentTitle title="Edit Authenticators">
    <PageHeader title="Edit Authenticators" subactions={<AuthenticatorActionLinks />}>
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

    <AuthenticatorsEdit />
  </DocumentTitle>
);

export default AuthenticatorsEditPage;
