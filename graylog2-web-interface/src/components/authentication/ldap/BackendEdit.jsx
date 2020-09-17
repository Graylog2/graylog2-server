// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import URI from 'urijs';

import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import type { LdapBackend, LdapCreate } from 'logic/authentication/ldap/types';
import { DocumentTitle } from 'components/common';

import WizardPageHeader from './WizardPageHeader';

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
  const initialValues = prepareInitialValues(authenticationBackend);
  const optionalProps = _optionalWizardProps(initialStepKey);
  const _handleSubmit = (payload: LdapCreate) => AuthenticationDomain.update(authenticationBackend.id,
    {
      ...payload,
      id: authenticationBackend.id,
    });

  return (
    <DocumentTitle title="Edit LDAP Authentication Service">
      <WizardPageHeader authenticationBackend={authenticationBackend} />
      <BackendWizard {...optionalProps}
                     initialValues={initialValues}
                     onSubmit={_handleSubmit}
                     authServiceType={authenticationBackend.config.type}
                     editing />
    </DocumentTitle>
  );
};

export default BackendEdit;
