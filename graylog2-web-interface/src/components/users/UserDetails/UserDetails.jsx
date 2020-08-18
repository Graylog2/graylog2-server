// @flow strict
import * as React from 'react';

import { IfPermitted, Spinner } from 'components/common';
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
        <IfPermitted permissions={`users:edit:${user.username}`}>
          <div>
            <ProfileSection user={user} />
            <IfPermitted permissions="*">
              <SettingsSection user={user} />
            </IfPermitted>
          </div>
          <div>
            <IfPermitted permissions={`users:rolesedit:${user.username}`}>
              <RolesSection user={user} />
            </IfPermitted>
            <IfPermitted permissions={`teams:edit:${user.username}`}>
              <TeamsSection user={user} />
            </IfPermitted>
          </div>
        </IfPermitted>
      </SectionGrid>
      <SharedEntitiesSection paginatedUserShares={paginatedUserShares}
                             username={user.username} />
    </>
  );
};

export default UserDetails;
