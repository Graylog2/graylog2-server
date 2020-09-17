// @flow strict
import * as React from 'react';

import { DocumentTitle } from 'components/common';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import type { LdapBackend, LdapCreate } from 'logic/authentication/ldap/types';

import WizardPageHeader from './WizardPageHeader';
import { HELP } from './BackendCreate';

import { prepareInitialValues } from '../ldap/BackendEdit';
import BackendWizard from '../BackendWizard';

type Props = {
  authenticationBackend: LdapBackend,
  initialStepKey: ?string,
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
    <DocumentTitle title="Edit Active Directory Authentication Service">
      <WizardPageHeader authenticationBackend={authenticationBackend} />
      <BackendWizard {...optionalProps}
                     initialValues={initialValues}
                     help={HELP}
                     onSubmit={_handleSubmit}
                     authServiceType={authenticationBackend.config.type}
                     editing />
    </DocumentTitle>
  );
};

export default BackendEdit;
