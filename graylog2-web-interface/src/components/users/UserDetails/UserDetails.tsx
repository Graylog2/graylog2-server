/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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

import PermissionsUpdateInfo from '../PermissionsUpdateInfo';

type Props = {
  user: User | null | undefined,
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
            <PermissionsUpdateInfo />
            <IfPermitted permissions={`users:rolesedit:${user.username}`}>
              <RolesSection user={user} />
            </IfPermitted>
            <IfPermitted permissions={`teams:edit:${user.username}`}>
              <TeamsSection user={user} />
            </IfPermitted>
          </div>
        </IfPermitted>
      </SectionGrid>
      <SharedEntitiesSection userId={user.id} />
    </>
  );
};

export default UserDetails;
