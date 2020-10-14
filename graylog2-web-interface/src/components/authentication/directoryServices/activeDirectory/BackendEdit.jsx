// @flow strict
import * as React from 'react';

import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import { DocumentTitle, Spinner } from 'components/common';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';

import WizardPageHeader from './WizardPageHeader';
import { HELP, AUTH_BACKEND_META } from './BackendCreate';

import { prepareInitialValues, handleSubmit } from '../ldap/BackendEdit';
import BackendWizard from '../BackendWizard';

type Props = {
  authenticationBackend: DirectoryServiceBackend,
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
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const groupSyncFormHelp = enterpriseGroupSyncPlugin?.help?.activeDirectory ?? {};
  const help = { ...HELP, ...groupSyncFormHelp };
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
    <DocumentTitle title="Edit Active Directory Authentication Service">
      <WizardPageHeader authenticationBackend={authenticationBackend} />
      <BackendWizard {..._optionalWizardProps(initialStepKey)}
                     authBackendMeta={authBackendMeta}
                     help={help}
                     initialValues={initialValues}
                     onSubmit={_handleSubmit} />
    </DocumentTitle>
  );
};

export default BackendEdit;
