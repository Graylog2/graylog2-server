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
import { useState, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import UsersDomain from 'domainActions/users/UsersDomain';
import { saveUserPreferences, type PreferencesUpdateMap } from 'api/preferences';
import useCurrentUser from 'hooks/useCurrentUser';
import { Col, Row, SegmentedControl, Alert } from 'components/bootstrap';
import { Spinner, IfPermitted } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';
import type User from 'logic/users/User';
import CurrentUser from 'logic/users/CurrentUser';
import TelemetrySettingsConfig from 'logic/telemetry/TelemetrySettingsConfig';
import { USERS_QUERY_KEY, type UserUpdate } from 'hooks/useUsers';

import ReadOnlyWarning from './ReadOnlyWarning';
import SettingsSection from './SettingsSection';
import PasswordSection from './PasswordSection';
import ProfileSection from './ProfileSection';
import PreferencesSection from './PreferencesSection';
import RolesSection from './RolesSection';
import TeamsSection from './TeamsSection';

import type { UserSegment } from '../UserDetails/UserDetails';
import { editableUserSegments } from '../UserDetails/UserDetails';
import PermissionsUpdateInfo from '../PermissionsUpdateInfo';

type Props = {
  user: User;
};

const UserEdit = ({ user }: Props) => {
  const currentUser = useCurrentUser();
  const queryClient = useQueryClient();
  const [selectedSegment, setSelectedSegment] = useState<UserSegment>('profile');
  const updateUser = useCallback(
    (data: UserUpdate) =>
      UsersDomain.update(user.id, { ...user.toJSON(), ...data }, user.fullName).then(() => {
        if (user.id === currentUser?.id) {
          CurrentUser.reload();
        }
      }),
    [user, currentUser?.id],
  );

  // Reload the edited user after saving, otherwise the preferences section shows the old values after switching
  // sections. The save only finishes once the reloaded user is fetched.
  const updatePreferences = (data: PreferencesUpdateMap) =>
    saveUserPreferences(user.username, data).then(() =>
      queryClient.invalidateQueries({ queryKey: [...USERS_QUERY_KEY, user.id] }),
    );

  if (!user) {
    return <Spinner />;
  }

  if (user.readOnly) {
    return <ReadOnlyWarning fullName={user.fullName} />;
  }

  return (
    <Row className="content">
      <Col md={12}>
        <SegmentedControl<UserSegment>
          data={editableUserSegments}
          value={selectedSegment}
          onChange={setSelectedSegment}
        />
      </Col>
      <Col md={12}>
        <IfPermitted permissions={`users:edit:${user.username}`}>
          {selectedSegment === 'profile' && (
            <>
              {user.external && (
                <SectionComponent title="External User">
                  <Alert bsStyle="warning">
                    This user was synced from an external server, therefore neither the profile nor the password can be
                    changed. Please contact your administrator for more information.
                  </Alert>
                </SectionComponent>
              )}
              {!user.external && <ProfileSection user={user} onSubmit={(data) => updateUser(data)} />}
              <IfPermitted permissions={`users:passwordchange:${user.username}`}>
                {!user.external && <PasswordSection user={user} />}
              </IfPermitted>
            </>
          )}
          {selectedSegment === 'settings_preferences' && (
            <>
              <SettingsSection user={user} onSubmit={(data) => updateUser(data)} />
              <PreferencesSection user={user} onSubmit={updatePreferences} />
              {currentUser.id === user.id && <TelemetrySettingsConfig />}
            </>
          )}
        </IfPermitted>

        {selectedSegment === 'teams_roles' && (
          <>
            <PermissionsUpdateInfo />
            <IfPermitted permissions={`users:rolesedit:${user.username}`}>
              <RolesSection user={user} onSubmit={(data) => updateUser(data)} />
            </IfPermitted>
            <IfPermitted permissions="team:edit">
              <TeamsSection user={user} />
            </IfPermitted>
          </>
        )}
      </Col>
    </Row>
  );
};

export default UserEdit;
