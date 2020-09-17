// @flow strict
import * as React from 'react';
import { Link } from 'react-router';

import { ReadOnlyFormGroup } from 'components/common';
import Routes from 'routing/Routes';
import SectionComponent from 'components/common/Section/SectionComponent';
import type { LdapBackend } from 'logic/authentication/ldap/types';

type Props = {
  authenticationBackend: LdapBackend,
};

const ServerConfigSection = ({ authenticationBackend }: Props) => {
  const { serverUrls = [], systemUserDn } = authenticationBackend.config;
  const editLink = {
    pathname: Routes.SYSTEM.AUTHENTICATION.PROVIDERS.edit(authenticationBackend.id),
    query: {
      initialStepKey: 'serverConfig',
    },
  };

  return (
    <SectionComponent title="Server Configuration" headerActions={<Link to={editLink}>Edit</Link>}>
      <ReadOnlyFormGroup label="Server Address" value={serverUrls[0]} />
      <ReadOnlyFormGroup label="System Username" value={systemUserDn} />
      <ReadOnlyFormGroup label="System Password" value="******" />
    </SectionComponent>
  );
};

export default ServerConfigSection;
