// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { DocumentTitle } from 'components/common';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import type { LdapBackend, LdapCreate } from 'logic/authentication/ldap/types';

import WizardPageHeader from './WizardPageHeader';
import { HELP, AUTH_BACKEND_META } from './BackendCreate';

import { prepareInitialValues, passwordUpdatePayload } from '../ldap/BackendEdit';
import type { WizardFormValues } from '../BackendWizard/contexts/BackendWizardContext';
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
  const authGroupSyncPlugins = PluginStore.exports('authentication.groupSync');
  const groupSyncFormValues = authGroupSyncPlugins?.[0]?.hooks?.useBackendFormValues;
  const backendHasGroupSync = !!groupSyncFormValues;
  const initialValues = { ...prepareInitialValues(authenticationBackend), ...groupSyncFormValues, synchronizeGroups: backendHasGroupSync };
  const optionalProps = _optionalWizardProps(initialStepKey);
  const authBackendMeta = { ...AUTH_BACKEND_META, backendId: authenticationBackend.id };

  const _handleSubmit = (payload: LdapCreate, formValues: WizardFormValues) => {
    return AuthenticationDomain.update(authenticationBackend.id, {
      ...payload,
      id: authenticationBackend.id,
      config: {
        ...payload.config,
        system_user_password: passwordUpdatePayload(payload.config.system_user_password),
      },
    }).then((result) => {
      if (formValues.synchronizeGroups && authGroupSyncPlugins?.[0]?.actions?.handleUpdate) {
        // Create and update group sync config
        return authGroupSyncPlugins[0].actions.handleUpdate(formValues, authBackendMeta);
      }

      if (backendHasGroupSync && !formValues.synchronizeGroups && authGroupSyncPlugins?.[0]?.actions?.delete) {
        // Delete existing group sync config
        return authGroupSyncPlugins[0].actions.delete(authenticationBackend.id);
      }

      return result;
    });
  };

  return (
    <DocumentTitle title="Edit Active Directory Authentication Service">
      <WizardPageHeader authenticationBackend={authenticationBackend} />
      <BackendWizard {...optionalProps}
                     authBackendMeta={authBackendMeta}
                     help={HELP}
                     initialValues={initialValues}
                     onSubmit={_handleSubmit} />
    </DocumentTitle>
  );
};

export default BackendEdit;
