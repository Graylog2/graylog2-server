// @flow strict
import * as React from 'react';

import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import { ReadOnlyFormGroup } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

import EditLinkButton from './EditLinkButton';

import { STEP_KEY as SERVER_CONFIG_KEY } from '../BackendWizard/ServerConfigStep';

type Props = {
  authenticationBackend: DirectoryServiceBackend,
};

const ServerConfigSection = ({ authenticationBackend }: Props) => {
  const { title, description, config: { servers = [], systemUserDn, systemUserPassword, transportSecurity, verifyCertificates } } = authenticationBackend;
  const serverUrls = servers.map((server) => `${server.host}:${server.port}`).join(', ');

  return (
    <SectionComponent title="Server Configuration" headerActions={<EditLinkButton authenticationBackendId={authenticationBackend.id} stepKey={SERVER_CONFIG_KEY} />}>
      <ReadOnlyFormGroup label="Title" value={title} />
      <ReadOnlyFormGroup label="Description" value={description} />
      <ReadOnlyFormGroup label="Server Address" value={serverUrls} />
      <ReadOnlyFormGroup label="System Username" value={systemUserDn} />
      <ReadOnlyFormGroup label="System Password" value={systemUserPassword?.isSet ? '******' : null} />
      <ReadOnlyFormGroup label="Transport Security" value={transportSecurity} />
      <ReadOnlyFormGroup label="Verify Certificates" value={verifyCertificates} />
    </SectionComponent>
  );
};

export default ServerConfigSection;
