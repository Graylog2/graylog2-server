// @flow strict
import * as React from 'react';

import SectionComponent from 'components/common/Section/SectionComponent';
import { ReadOnlyFormGroup } from 'components/common';

import type { LdapService } from '../types';

type Props = {
  authenticationService: LdapService,
};

const ServerConfigSection = ({ authenticationService }: Props) => {
  const { serverUri, systemUsername } = authenticationService;

  return (
    <SectionComponent title="Server Configuration">
      <ReadOnlyFormGroup label="Server Address" value={serverUri} />
      <ReadOnlyFormGroup label="Server Username" value={systemUsername} />
      <ReadOnlyFormGroup label="Server Password" value="******" />
    </SectionComponent>
  );
};

export default ServerConfigSection;
