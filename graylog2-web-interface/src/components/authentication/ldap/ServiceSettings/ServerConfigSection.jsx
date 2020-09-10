// @flow strict
import * as React from 'react';
import { Link } from 'react-router';

import SectionComponent from 'components/common/Section/SectionComponent';
import { ReadOnlyFormGroup } from 'components/common';
import Routes from 'routing/Routes';

import type { LdapService } from '../types';

type Props = {
  authenticationService: LdapService,
};

const ServerConfigSection = ({ authenticationService }: Props) => {
  const { serverUri, systemUsername } = authenticationService.config;
  const editLink = {
    pathname: Routes.SYSTEM.AUTHENTICATION.PROVIDERS.edit(authenticationService.id),
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
