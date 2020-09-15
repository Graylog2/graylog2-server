// @flow strict
import * as React from 'react';
import { Link } from 'react-router';

import SectionComponent from 'components/common/Section/SectionComponent';
import { ReadOnlyFormGroup } from 'components/common';
import Routes from 'routing/Routes';
import type { LdapBackend } from 'logic/authentication/ldap/types';

type Props = {
  authenticationBackend: LdapBackend,
};

const ServerConfigSection = ({ authenticationBackend }: Props) => {
  const { serverUri, systemUsername } = authenticationBackend.config;
  const editLink = {
    pathname: Routes.SYSTEM.AUTHENTICATION.PROVIDERS.edit(authenticationBackend.id),
    query: {
      step: 'serverConfig',
    },
  };

  return (
    <SectionComponent title="Server Configuration" headerActions={<Link to={editLink}>Edit</Link>}>
      <ReadOnlyFormGroup label="Server Address" value={serverUri} />
      <ReadOnlyFormGroup label="Server Username" value={systemUsername} />
      <ReadOnlyFormGroup label="Server Password" value="******" />
    </SectionComponent>
  );
};

export default ServerConfigSection;
