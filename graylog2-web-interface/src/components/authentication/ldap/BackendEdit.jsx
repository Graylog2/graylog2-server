// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import URI from 'urijs';

import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import type { LdapBackend, LdapCreate } from 'logic/authentication/ldap/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { DocumentTitle, Spinner } from 'components/common';

import { AUTH_BACKEND_META } from './BackendCreate';
import WizardPageHeader from './WizardPageHeader';

import type { WizardFormValues } from '../BackendWizard/contexts/BackendWizardContext';
import BackendWizard from '../BackendWizard';

type Props = {
  authenticationBackend: LdapBackend,
  initialStepKey: ?string,
};

export const prepareInitialValues = ({
  defaultRoles = Immutable.List(),
  config: {
    serverUrls = [],
    systemUserDn,
    transportSecurity,
    userFullNameAttribute,
    userNameAttribute,
    userSearchBase,
    userSearchPattern,
    verifyCertificates,
  },
}: LdapBackend): WizardFormValues => {
  const serverUrl = new URI(serverUrls[0]);

  return {
    defaultRoles: defaultRoles.join(),
    serverUrlHost: serverUrl.hostname(),
    serverUrlPort: serverUrl.port(),
    systemUserDn,
    transportSecurity,
    userFullNameAttribute,
    userNameAttribute,
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

export const handleSubmit = (payload: LdapCreate, formValues: WizardFormValues, backendId: string, backendGroupSyncIsActive: boolean, serviceType: string) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();

  return AuthenticationDomain.update(backendId, {
    ...payload,
    id: backendId,
  }).then((result) => {
    if (result && enterpriseGroupSyncPlugin) {
      return enterpriseGroupSyncPlugin.actions.handleBackendUpdate(backendGroupSyncIsActive, formValues, backendId, serviceType);
    }

    return result;
  });
};

const BackendEdit = ({ authenticationBackend, initialStepKey }: Props) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  let initialValues = prepareInitialValues(authenticationBackend);

  if (enterpriseGroupSyncPlugin) {
    const {
      initialValues: initialGroupSyncValues,
      finishedLoading,
    } = enterpriseGroupSyncPlugin.hooks.useInitialGroupSyncValues(authenticationBackend.id);

    if (!finishedLoading) {
      return <Spinner />;
    }

    initialValues = { ...initialValues, ...initialGroupSyncValues };
  }

  const authBackendMeta = {
    ...AUTH_BACKEND_META,
    backendId: authenticationBackend.id,
    backendHasPassword: authenticationBackend.config.systemUserPassword.isSet,
    backendGroupSyncIsActive: !!initialValues.synchronizeGroups,
  };
  const _handleSubmit = (payload, formValues) => handleSubmit(payload, formValues, authenticationBackend.id, !!initialValues.synchronizeGroups, authBackendMeta.serviceType);

  return (
    <DocumentTitle title="Edit LDAP Authentication Service">
      <WizardPageHeader authenticationBackend={authenticationBackend} />
      <BackendWizard {..._optionalWizardProps(initialStepKey)}
                     authBackendMeta={authBackendMeta}
                     initialValues={initialValues}
                     onSubmit={_handleSubmit} />
    </DocumentTitle>
  );
};

export default BackendEdit;
