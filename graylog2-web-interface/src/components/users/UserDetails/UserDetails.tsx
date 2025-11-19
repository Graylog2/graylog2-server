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
import React, { useState } from 'react';

import { Col, Row, SegmentedControl } from 'components/bootstrap';
import { IfPermitted, Spinner } from 'components/common';
import type User from 'logic/users/User';
import { isPermitted } from 'util/PermissionsMixin';
import TelemetrySettingsDetails from 'logic/telemetry/TelemetrySettingsDetails';
import useCurrentUser from 'hooks/useCurrentUser';
import TelemetrySettingsConfig from 'logic/telemetry/TelemetrySettingsConfig';

import PreferencesSection from './PreferencesSection';
import ProfileSection from './ProfileSection';
import RolesSection from './RolesSection';
import SettingsSection from './SettingsSection';
import SharedEntitiesSection from './SharedEntitiesSection';
import TeamsSection from './TeamsSection';
import CollectionsSection from './CollectionsSection';

import PermissionsUpdateInfo from '../PermissionsUpdateInfo';

type Props = {
  user: User | null | undefined;
};

export type UserSegment = 'profile' | 'settings_preferences' | 'collections' | 'teams_roles' | 'shared_entities';

export const editableUserSegments: Array<{ value: UserSegment; label: string }> = [
  { value: 'profile', label: 'Profile' },
  { value: 'settings_preferences', label: 'Preferences' },
  { value: 'teams_roles', label: 'Teams & Roles' },
];

const UserDetails = ({ user }: Props) => {
  const currentUser = useCurrentUser();

  const userSegments: Array<{ value: UserSegment; label: string }> = [
    ...editableUserSegments,
    { value: 'collections', label: 'Collections' },
    { value: 'shared_entities', label: 'Shared Entities' },
  ];
  const editPermissionRequiredSections = ['profile', 'settings_preferences', 'teams_roles', 'collections'];

  const filteredUserSegments = () => {
    if (isPermitted(currentUser.permissions, `users:edit:${user?.username}`)) {
      return userSegments;
    }

    return userSegments.filter((userSegment) => editPermissionRequiredSections.includes(userSegment.value));
  };
  const [selectedSegment, useSelectedSegment] = useState<UserSegment>(filteredUserSegments()[0].value);

  const isLocalAdmin = currentUser.id === 'local:admin';

  if (!user) {
    return <Spinner />;
  }

  return (
    <Row className="content">
      <Col md={12}>
        <SegmentedControl<UserSegment> data={userSegments} value={selectedSegment} onChange={useSelectedSegment} />
      </Col>
      <Col md={12}>
        {selectedSegment === 'profile' && <ProfileSection user={user} />}
        {selectedSegment === 'settings_preferences' && (
          <>
            <IfPermitted permissions="*">
              <SettingsSection user={user} />
            </IfPermitted>
            <PreferencesSection user={user} />

            {currentUser.id === user.id && !isLocalAdmin && <TelemetrySettingsDetails />}
            {currentUser.id === user.id && isLocalAdmin && <TelemetrySettingsConfig />}
          </>
        )}
        {selectedSegment === 'teams_roles' && (
          <>
            <PermissionsUpdateInfo />
            <IfPermitted permissions={`users:rolesedit:${user.username}`}>
              <RolesSection user={user} />
            </IfPermitted>
            <IfPermitted permissions={`team:edit:${user.username}`}>
              <TeamsSection user={user} />
            </IfPermitted>
          </>
        )}
        {selectedSegment === 'collections' && <CollectionsSection user={user} />}
        {selectedSegment === 'shared_entities' && <SharedEntitiesSection userId={user.id} />}
      </Col>
    </Row>
  );
};

export default UserDetails;
