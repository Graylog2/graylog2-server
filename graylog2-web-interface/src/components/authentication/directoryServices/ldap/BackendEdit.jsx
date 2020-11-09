// @flow strict
import * as React from 'react';

import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import { DocumentTitle, Spinner } from 'components/common';

import { AUTH_BACKEND_META, HELP } from './BackendCreate';
import WizardPageHeader from './WizardPageHeader';

import prepareInitialWizardValues from '../PrepareInitialWizardValues';
import BackendWizard from '../BackendWizard';
import handleUpdate from '../HandleUpdate';

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
  const {
    help: groupSyncHelp = {},
    initialValues: initialGroupSyncValues = {},
  } = enterpriseGroupSyncPlugin?.wizardConfig?.ldap ?? {};
  const help = { ...HELP, ...groupSyncHelp };
  let initialValues = prepareInitialWizardValues(authenticationBackend);

  if (enterpriseGroupSyncPlugin) {
    const {
      formValues: groupSyncFormValues,
      finishedLoading,
    } = enterpriseGroupSyncPlugin.hooks.useInitialGroupSyncValues(authenticationBackend.id, initialGroupSyncValues);

    if (!finishedLoading) {
      return <Spinner />;
    }

    initialValues = { ...initialValues, ...groupSyncFormValues };
  }

  const authBackendMeta = {
    ...AUTH_BACKEND_META,
    backendId: authenticationBackend.id,
    backendHasPassword: authenticationBackend.config.systemUserPassword.isSet,
    backendGroupSyncIsActive: !!initialValues.synchronizeGroups,
  };
  const _handleSubmit = (
    payload,
    formValues,
    serviceType,
    shouldUpdateGroupSync,
  ) => handleUpdate(
    payload,
    formValues,
    authenticationBackend.id,
    !!initialValues.synchronizeGroups,
    serviceType,
    shouldUpdateGroupSync,
  );

  return (
    <DocumentTitle title="Edit LDAP Authentication Service">
      <WizardPageHeader authenticationBackend={authenticationBackend} />
      <BackendWizard {..._optionalWizardProps(initialStepKey)}
                     help={help}
                     authBackendMeta={authBackendMeta}
                     initialValues={initialValues}
                     onSubmit={_handleSubmit} />
    </DocumentTitle>
  );
};

export default BackendEdit;
