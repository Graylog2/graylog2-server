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

const UserSyncSection = ({ authenticationBackend }: Props) => {
  const {
    userSearchBase,
    userSearchPattern,
    userNameAttribute,
    userFullNameAttribute,
  } = authenticationBackend.config;
  const {
    defaultRoles = [],
  } = authenticationBackend;
  const editLink = {
    pathname: Routes.SYSTEM.AUTHENTICATION.PROVIDERS.edit(authenticationBackend.id),
    query: {
      initialStepKey: 'userSync',
    },
  };

  return (
    <SectionComponent title="User Synchronisation" headerActions={<Link to={editLink}>Edit</Link>}>
      <ReadOnlyFormGroup label="Search Base DN" value={userSearchBase} />
      <ReadOnlyFormGroup label="Search Pattern" value={userSearchPattern} />
      <ReadOnlyFormGroup label="Name Attribute" value={userNameAttribute} />
      <ReadOnlyFormGroup label="Full Name Attribute" value={userFullNameAttribute} />
      <ReadOnlyFormGroup label="Default Roles" value={defaultRoles.join(', ')} />
    </SectionComponent>
  );
};

export default UserSyncSection;
