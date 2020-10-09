// @flow strict
import * as React from 'react';

import type { LdapBackend } from 'logic/authentication/ldap/types';
import { ReadOnlyFormGroup } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

import EditLinkButton from './EditLinkButton';

import { STEP_KEY as SERVER_CONFIG_KEY } from '../../BackendWizard/ServerConfigStep';

type Props = {
  authenticationBackend: LdapBackend,
};

const ServerConfigSection = ({ authenticationBackend }: Props) => {
  const { serverUrls = [], systemUserDn, systemUserPassword, transportSecurity, verifyCertificates } = authenticationBackend.config;

  return (
    <SectionComponent title="Server Configuration" headerActions={<EditLinkButton authenticationBackendId={authenticationBackend.id} stepKey={SERVER_CONFIG_KEY} />}>
      <ReadOnlyFormGroup label="Server Address" value={serverUrls.join(', ')} />
      <ReadOnlyFormGroup label="System Username" value={systemUserDn} />
      <ReadOnlyFormGroup label="System Password" value={systemUserPassword?.isSet ? '******' : null} />
      <ReadOnlyFormGroup label="Transport Security" value={transportSecurity} />
      <ReadOnlyFormGroup label="Verify Certificates" value={verifyCertificates} />
    </SectionComponent>
  );
};

export default ServerConfigSection;
