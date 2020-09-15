// @flow strict
import * as React from 'react';
import URI from 'urijs';

import DocsHelper from 'util/DocsHelper';
import { PageHeader, DocumentTitle } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import type { LdapBackend, LdapCreate } from 'logic/authentication/ldap/types';

import BackendWizard from '../BackendWizard';

type Props = {
  authenticationBackend: LdapBackend,
  initialStep: ?string,
};

export const prepareInitialValues = ({
  defaultRoles,
  displayNameAttribute,
  serverUri,
  systemUsername,
  trustAllCertificates,
  userSearchBase,
  userSearchPattern,
  useStartTls,
  useSsl,
}: $PropertyType<LdapBackend, 'config'>) => {
  const serverUriObj = new URI(serverUri);

  return {
    defaultRoles,
    displayNameAttribute,
    systemUsername,
    trustAllCertificates,
    userSearchBase,
    userSearchPattern,
    useStartTls,
    useSsl,
    serverUriHost: serverUriObj.hostname(),
    serverUriPort: serverUriObj.port(),
  };
};

const _optionalWizardProps = (initialStep: ?string) => {
  const props = {};

  if (initialStep) {
    props.initialStep = initialStep;
  }

  return props;
};

const BackendEdit = ({ authenticationBackend, initialStep }: Props) => {
  const initialValues = prepareInitialValues(authenticationBackend.config);
  const optionalProps = _optionalWizardProps(initialStep);
  const _handleSubmit = (payload: LdapCreate) => AuthenticationDomain.update(authenticationBackend.id,
    {
      ...payload,
      id: authenticationBackend.id,
    });

  return (
    <DocumentTitle title="Edit LDAP Authentication Service">
      <PageHeader title="Edit LDAP Authentication Service">
        <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
        <span>
          Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                             text="documentation" />.
        </span>
        <BackendOverviewLinks />
      </PageHeader>
      <BackendWizard {...optionalProps}
                     initialValues={initialValues}
                     onSubmit={_handleSubmit}
                     authServiceType="ldap"
                     editing />
    </DocumentTitle>
  );
};

export default BackendEdit;
