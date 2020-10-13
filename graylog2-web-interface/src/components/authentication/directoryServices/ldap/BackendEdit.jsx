// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

import type { DirectoryServiceBackend, WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { DocumentTitle, Spinner } from 'components/common';

import { AUTH_BACKEND_META } from './BackendCreate';
import WizardPageHeader from './WizardPageHeader';

import type { WizardFormValues } from '../BackendWizard//BackendWizardContext';
import BackendWizard from '../BackendWizard';

type Props = {
  authenticationBackend: DirectoryServiceBackend,
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
}: DirectoryServiceBackend): WizardFormValues => {
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

const _optionalWizardProps = (initialStepKey: ?string) => {
  const props = {};

  if (initialStepKey) {
    props.initialStepKey = initialStepKey;
  }

  return props;
};

export const handleSubmit = (payload: WizardSubmitPayload, formValues: WizardFormValues, backendId: string, backendGroupSyncIsActive: boolean, serviceType: string) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();

  return AuthenticationDomain.update(backendId, {
    ...payload,
    id: backendId,
  }).then((result) => {
    if (result && enterpriseGroupSyncPlugin) {
      return enterpriseGroupSyncPlugin.actions.onDirectoryServiceBackendUpdate(backendGroupSyncIsActive, formValues, backendId, serviceType);
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
