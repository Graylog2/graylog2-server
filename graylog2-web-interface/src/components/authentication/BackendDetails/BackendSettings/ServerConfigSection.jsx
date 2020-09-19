// @flow strict
import * as React from 'react';
import { Link } from 'react-router';

import { ReadOnlyFormGroup } from 'components/common';
import Routes from 'routing/Routes';
import SectionComponent from 'components/common/Section/SectionComponent';
import type { LdapBackend } from 'logic/authentication/ldap/types';

import { STEP_KEY as SERVER_CONFIG_KEY } from '../../BackendWizard/ServerConfigStep';

type Props = {
  authenticationBackend: LdapBackend,
};

const ServerConfigSection = ({ authenticationBackend }: Props) => {
  const { serverUrls = [], systemUserDn, transportSecurity, verifyCertificates } = authenticationBackend.config;
  const editLink = {
    pathname: Routes.SYSTEM.AUTHENTICATION.PROVIDERS.edit(authenticationBackend.id),
    query: {
      initialStepKey: SERVER_CONFIG_KEY,
    },
  };

  return (
    <SectionComponent title="Server Configuration" headerActions={<Link to={editLink}>Edit</Link>}>
      <ReadOnlyFormGroup label="Server Address" value={serverUrls.join(', ')} />
      <ReadOnlyFormGroup label="System Username" value={systemUserDn} />
      <ReadOnlyFormGroup label="System Password" value="******" />
      <ReadOnlyFormGroup label="Transport Security" value={transportSecurity} />
      <ReadOnlyFormGroup label="Verify Certificates" value={verifyCertificates} />
    </SectionComponent>
  );
};

export default ServerConfigSection;
