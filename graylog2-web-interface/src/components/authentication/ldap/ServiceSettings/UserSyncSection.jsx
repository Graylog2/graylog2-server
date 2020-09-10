// @flow strict
import * as React from 'react';

import SectionComponent from 'components/common/Section/SectionComponent';
import { ReadOnlyFormGroup } from 'components/common';

import type { LdapService } from '../types';

type Props = {
  authenticationService: LdapService,
};

const UserSyncSection = ({ authenticationService }: Props) => {
  const { userSearchBase, userSearchPattern, displayNameAttribute } = authenticationService;

  return (
    <SectionComponent title="User Synchronisation">
      <ReadOnlyFormGroup label="Search Base DN" value={userSearchBase} />
      <ReadOnlyFormGroup label="User Search Pattern" value={userSearchPattern} />
      <ReadOnlyFormGroup label="Display Name Attribute" value={displayNameAttribute} />
    </SectionComponent>
  );
};

export default UserSyncSection;
