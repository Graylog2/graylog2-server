// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import type { LdapBackend } from 'logic/authentication/ldap/types';
import { DocumentTitle, Spinner } from 'components/common';

import WizardPageHeader from './WizardPageHeader';
import { HELP, AUTH_BACKEND_META } from './BackendCreate';

import { prepareInitialValues, handleSubmit } from '../ldap/BackendEdit';
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
  const authGroupSyncPlugins = PluginStore.exports('authentication.enterprise.ldap.groupSync');
  const hasGroupSyncPlugin = !!authGroupSyncPlugins?.[0];
  const authBackendMeta = { ...AUTH_BACKEND_META, backendId: authenticationBackend.id };
  let initialValues = prepareInitialValues(authenticationBackend);

  if (hasGroupSyncPlugin) {
    const {
      initialValues: initialGroupSyncValues,
      finishedLoading,
    } = authGroupSyncPlugins?.[0]?.hooks?.useInitialGroupSyncValues(authenticationBackend.id);

    if (!finishedLoading) {
      return <Spinner />;
    }

    initialValues = { ...initialValues, ...initialGroupSyncValues };
  }

  const _handleSubmit = (payload, formValues) => handleSubmit(payload, formValues, authBackendMeta.backendId, !!initialValues.synchronizeGroups, authBackendMeta.serviceType);

  return (
    <DocumentTitle title="Edit Active Directory Authentication Service">
      <WizardPageHeader authenticationBackend={authenticationBackend} />
      <BackendWizard {..._optionalWizardProps(initialStepKey)}
                     authBackendMeta={authBackendMeta}
                     help={HELP}
                     initialValues={initialValues}
                     onSubmit={_handleSubmit} />
    </DocumentTitle>
  );
};

export default BackendEdit;
