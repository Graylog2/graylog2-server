// @flow strict
import * as React from 'react';

import { Spinner } from 'components/common';
import User from 'logic/users/User';
import type { PaginatedUserSharesType } from 'stores/permissions/EntityShareStore';

import SettingsSection from './SettingsSection';
import ProfileSection from './ProfileSection';
import MainDetailsGrid from './MainDetailsGrid';
import RolesSection from './RolesSection';
import TeamsSection from './TeamSection';
import SharedEntitiesSection from './SharedEntitiesSection';

type Props = {
  user: ?User,
  paginatedUserShares: PaginatedUserSharesType,
};

const UserDetails = ({ user, paginatedUserShares }: Props) => {
  if (!user) {
    return <Spinner />;
  }

  return (
    <>
      <MainDetailsGrid>
        <div>
          <ProfileSection user={user} />
          <SettingsSection user={user} />
        </div>
        <div>
          <RolesSection user={user} />
          <TeamsSection user={user} />
        </div>
      </MainDetailsGrid>
      <SharedEntitiesSection paginatedUserShares={paginatedUserShares} username={user.username} />
    </>
  );
};

export default UserDetails;
