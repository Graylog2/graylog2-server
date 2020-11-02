// @flow strict
import React from 'react';

import AuthenticatorActionLinks from 'components/authentication/AuthenticatorActionLinks';
import AuthenticatorsEdit from 'components/authentication/AuthenticatorsEdit';
import { PageHeader, DocumentTitle } from 'components/common';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import useActiveBackend from 'components/authentication/useActiveBackend';

const AuthenticatorsEditPage = () => {
  const { finishedLoading, activeBackend } = useActiveBackend();

  return (
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

        <BackendOverviewLinks activeBackend={activeBackend}
                              finishedLoading={finishedLoading} />

      </PageHeader>

      <AuthenticatorsEdit />
    </DocumentTitle>
  );
};

export default AuthenticatorsEditPage;
