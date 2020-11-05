// @flow strict
import * as React from 'react';

import { Spinner, IfPermitted } from 'components/common';
import Role from 'logic/roles/Role';

import UsersSection from './UsersSection';
import TeamsSection from './TeamsSection';

import ProfileSection from '../RoleDetails/ProfileSection';
import SectionGrid from '../../common/Section/SectionGrid';

type Props = {
  role: ?Role,
};

const RoleEdit = ({ role }: Props) => {
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
        <IfPermitted permissions="teams:edit">
          <TeamsSection role={role} />
        </IfPermitted>
      </div>
    </SectionGrid>
  );
};

export default RoleEdit;
