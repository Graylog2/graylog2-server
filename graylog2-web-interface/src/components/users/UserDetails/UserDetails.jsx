// @flow strict
import * as React from 'react';

import { Spinner } from 'components/common';
import User from 'logic/users/User';
import type { PaginatedEnititySharesType } from 'actions/permissions/EntityShareActions';
import SectionGrid from 'components/common/Section/SectionGrid';

import SettingsSection from './SettingsSection';
import ProfileSection from './ProfileSection';
import RolesSection from './RolesSection';
import TeamsSection from './TeamSection';
import SharedEntitiesSection from './SharedEntitiesSection';

type Props = {
  paginatedUserShares: ?PaginatedEnititySharesType,
  user: ?User,
};

const UserDetails = ({ user, paginatedUserShares }: Props) => {
  if (!user) {
    return <Spinner />;
  }

  return (
    <>
      <SectionGrid>
        <div>
          <ProfileSection user={user} />
          <SettingsSection user={user} />
        </div>
        <div>
          <RolesSection user={user} />
          <TeamsSection user={user} />
        </div>
      </SectionGrid>
      <SharedEntitiesSection paginatedUserShares={paginatedUserShares}
                             username={user.username} />
    </>
  );
};

export default UserDetails;
