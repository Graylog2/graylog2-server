// @flow strict
import * as React from 'react';

import { Spinner } from 'components/common';
import SectionGrid from 'components/common/Section/SectionGrid';
import Role from 'logic/roles/Role';

import ProfileSection from './ProfileSection';
import UsersSection from './UsersSection';

type Props = {
  role: ?Role,
};

const RoleDetails = ({ role }: Props) => {
  if (!role) {
    return <Spinner />;
  }

  return (
    <SectionGrid>
      <div>
        <ProfileSection role={role} />
      </div>
      <div>
        <UsersSection role={role} />
      </div>
    </SectionGrid>
  );
};

export default RoleDetails;
