// @flow strict
import * as React from 'react';

import { IfPermitted, Spinner } from 'components/common';
import User from 'logic/users/User';
import SectionGrid from 'components/common/Section/SectionGrid';

import PreferencesSection from './PreferencesSection';
import ProfileSection from './ProfileSection';
import RolesSection from './RolesSection';
import SettingsSection from './SettingsSection';
import SharedEntitiesSection from './SharedEntitiesSection';
import TeamsSection from './TeamsSection';

type Props = {
  user: ?User,
};

const UserDetails = ({ user }: Props) => {
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
            <PreferencesSection user={user} />
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
      <SharedEntitiesSection username={user.username} />
    </>
  );
};

export default UserDetails;
