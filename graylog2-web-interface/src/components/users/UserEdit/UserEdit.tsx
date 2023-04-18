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
import * as React from 'react';

import UsersDomain from 'domainActions/users/UsersDomain';
import useCurrentUser from 'hooks/useCurrentUser';
import { Spinner, IfPermitted } from 'components/common';
import { Alert } from 'components/bootstrap';
import SectionComponent from 'components/common/Section/SectionComponent';
import type User from 'logic/users/User';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import TelemetrySettingsConfig from 'logic/telemetry/TelemetrySettingsConfig';

import ReadOnlyWarning from './ReadOnlyWarning';
import SettingsSection from './SettingsSection';
import PasswordSection from './PasswordSection';
import ProfileSection from './ProfileSection';
import PreferencesSection from './PreferencesSection';
import RolesSection from './RolesSection';
import TeamsSection from './TeamsSection';

import PermissionsUpdateInfo from '../PermissionsUpdateInfo';
import SectionGrid from '../../common/Section/SectionGrid';

type Props = {
  user: User,
};

const _updateUser = (data, currentUser, userId, fullName) => {
  return UsersDomain.update(userId, data, fullName).then(() => {
    if (userId === currentUser?.id) {
      CurrentUserStore.reload();
    }
  });
};

const UserEdit = ({ user }: Props) => {
  const currentUser = useCurrentUser();

  if (!user) {
    return <Spinner />;
  }

  if (user.readOnly) {
    return <ReadOnlyWarning fullName={user.fullName} />;
  }

  return (
    <SectionGrid>
      <IfPermitted permissions={`users:edit:${user.username}`}>
        <div>
          {user.external && (
            <SectionComponent title="External User">
              <Alert bsStyle="warning">
                This user was synced from an external server, therefore neither
                the profile nor the password can be changed. Please contact your administrator for more information.
              </Alert>
            </SectionComponent>
          )}
          {!user.external && (
            <ProfileSection user={user}
                            onSubmit={(data) => _updateUser(data, currentUser, user.id, user.fullName)} />
          )}
          <SettingsSection user={user}
                           onSubmit={(data) => _updateUser(data, currentUser, user.id, user.fullName)} />
          <IfPermitted permissions={`users:passwordchange:${user.username}`}>
            {!user.external && <PasswordSection user={user} />}
          </IfPermitted>
          <PreferencesSection user={user} />
        </div>
        <div>
          <PermissionsUpdateInfo />
          <IfPermitted permissions="users:rolesedit">
            <RolesSection user={user}
                          onSubmit={(data) => _updateUser(data, currentUser, user.id, user.fullName)} />
          </IfPermitted>
          <IfPermitted permissions="teams:edit">
            <TeamsSection user={user} />
          </IfPermitted>
          {(currentUser.id === user.id) && (
            <IfPermitted permissions={`users:edit:${user.username}`}>
              <TelemetrySettingsConfig />
            </IfPermitted>
          )}
        </div>
      </IfPermitted>
    </SectionGrid>
  );
};

export default UserEdit;
