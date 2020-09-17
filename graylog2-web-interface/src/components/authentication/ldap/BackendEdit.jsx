// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import URI from 'urijs';

import DocsHelper from 'util/DocsHelper';
import { PageHeader, DocumentTitle } from 'components/common';
import { useActiveBackend } from 'components/authentication/hooks';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import type { LdapBackend, LdapCreate } from 'logic/authentication/ldap/types';

import BackendWizard from '../BackendWizard';

type Props = {
  authenticationBackend: LdapBackend,
  initialStepKey: ?string,
};

export const prepareInitialValues = ({
  defaultRoles = Immutable.Map(),
  config: {
    serverUrls = [],
    systemUserDn,
    systemUserPassword,
    transportSecurity,
    userFullNameAttribute,
    userNameAribute,
    userSearchBase,
    userSearchPattern,
    verifyCertificates,
  },
}: LdapBackend) => {
  const serverUrl = new URI(serverUrls[0]);

  return {
    defaultRoles: defaultRoles.join(),
    serverUrlHost: serverUrl.host(),
    serverUrlPort: serverUrl.port(),
    systemUserDn,
    systemUserPassword,
    transportSecurity,
    userFullNameAttribute,
    userNameAribute,
    userSearchBase,
    userSearchPattern,
    verifyCertificates,
  };
};

const _optionalWizardProps = (initialStepKey: ?string) => {
  const props = {};

  if (initialStepKey) {
    props.initialStepKey = initialStepKey;
  }

  return props;
};

const BackendEdit = ({ authenticationBackend, initialStepKey }: Props) => {
  const { finishedLoading, activeBackend } = useActiveBackend();
  const initialValues = prepareInitialValues(authenticationBackend);
  const optionalProps = _optionalWizardProps(initialStepKey);
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

        <BackendOverviewLinks activeBackend={activeBackend}
                              finishedLoading={finishedLoading} />
      </PageHeader>

      <BackendWizard {...optionalProps}
                     initialValues={initialValues}
                     onSubmit={_handleSubmit}
                     authServiceType={authenticationBackend.config.type}
                     editing />
    </DocumentTitle>
  );
};

export default BackendEdit;
