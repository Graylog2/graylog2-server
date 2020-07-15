// @flow strict
import * as React from 'react';

import { Spinner } from 'components/common';
import User from 'logic/users/User';

import SettingsSection from './SettingsSection';
import ProfileSection from './ProfileSection';
import MainDetailsGrid from './MainDetailsGrid';

import SectionComponent from '../SectionComponent';
import RolesSection from "./RolesSection";

type Props = {
  user: ?User,
};

const UserDetails = ({ user }: Props) => {
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
          <SectionComponent title="Teams">Children</SectionComponent>
          <RolesSection user={user} />
        </div>
      </MainDetailsGrid>
      <SectionComponent title="Entity Shares">Children</SectionComponent>
    </>
  );
};

export default UserDetails;
