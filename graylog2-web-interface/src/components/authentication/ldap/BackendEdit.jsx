// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

import type { LdapBackend, LdapCreate } from 'logic/authentication/ldap/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { DocumentTitle } from 'components/common';

import { AUTH_BACKEND_META } from './BackendCreate';
import WizardPageHeader from './WizardPageHeader';

import BackendWizard from '../BackendWizard';

type Props = {
  authenticationBackend: LdapBackend,
  initialStepKey: ?string,
};

export const prepareInitialValues = ({
  defaultRoles = Immutable.List(),
  config: {
    servers = [],
    systemUserDn,
    transportSecurity,
    userFullNameAttribute,
    userNameAttribute,
    userSearchBase,
    userSearchPattern,
    verifyCertificates,
  },
}: LdapBackend) => {
  return {
    defaultRoles: defaultRoles.join(),
    serverHost: servers[0].host,
    serverPort: servers[0].port,
    systemUserDn,
    transportSecurity,
    userFullNameAttribute,
    userNameAttribute,
    userSearchBase,
    userSearchPattern,
    verifyCertificates,
  };
};

export const passwordUpdatePayload = (systemUserPassword: ?string) => {
  // Only update password on edit if necessary,
  // if a users resets the previously defined password its form value is an empty string
  if (systemUserPassword === undefined) {
    return { keep_value: true };
  }

  if (systemUserPassword === '') {
    return { delete_value: true };
  }

  return { set_value: systemUserPassword };
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
      config: {
        ...payload.config,
        system_user_password: passwordUpdatePayload(payload.config.system_user_password),
      },
    });

  return (
    <DocumentTitle title="Edit LDAP Authentication Service">
      <WizardPageHeader authenticationBackend={authenticationBackend} />
      <BackendWizard {...optionalProps}
                     authBackendMeta={{
                       ...AUTH_BACKEND_META,
                       backendId: authenticationBackend.id,
                       backendHasPassword: authenticationBackend.config.systemUserPassword.isSet,
                     }}
                     initialValues={initialValues}
                     onSubmit={_handleSubmit} />
    </DocumentTitle>
  );
};

export default BackendEdit;
