// @flow strict
import * as React from 'react';
import URI from 'urijs';

import DocsHelper from 'util/DocsHelper';
import { PageHeader, DocumentTitle } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';

import BackendWizard from '../BackendWizard';

type Props = {
  authenticationBackend: AuthenticationBackend,
  initialStep: ?string,
};

const BackendEdit = ({ authenticationBackend, initialStep }: Props) => {
  console.log(authenticationBackend.config);
  const {
    defaultRoles,
    displayNameAttribute,
    encryptedSystemPassword,
    serverUri,
    systemUsername,
    trustAllCertificates,
    userSearchBase,
    userSearchPattern,
    useStartTls,
    useSsl,
  } = authenticationBackend.config;

  const serverUriObj = new URI(serverUri);

  const formValues = {
    defaultRoles,
    displayNameAttribute,
    systemUsername,
    trustAllCertificates,
    userSearchBase,
    userSearchPattern,
    useStartTls,
    useSsl,
    systemPassword: encryptedSystemPassword, // TMP
    serverUriHost: serverUriObj.hostname(),
    serverUriPort: serverUriObj.port(),
  };

  return (
    <DocumentTitle title="Edit LDAP Authentication Provider">
      <PageHeader title="Edit LDAP Authentication Provider">
        <span>Configure Graylog&apos;s authentication providers of this Graylog cluster.</span>
        <span>
          Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                             text="documentation" />.
        </span>
        <BackendOverviewLinks />
      </PageHeader>
      <BackendWizard onSubmitAll={() => {}} initialValues={formValues} initialStep={initialStep} editing />
    </DocumentTitle>
  );
};

export default BackendEdit;
