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
import * as Immutable from 'immutable';
import { useTranslation } from 'react-i18next';

import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import type Role from 'logic/roles/Role';
import { ReadOnlyFormGroup } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

import EditLinkButton from './EditLinkButton';

import { STEP_KEY as USER_SYNC_KEY } from '../BackendWizard/UserSyncStep';

const rolesList = (defaultRolesIds: Immutable.List<string>, roles: Immutable.List<Role>, roleNotFound: string) => {
  const defaultRolesNames = defaultRolesIds.map(
    (roleId) => roles.find((role) => role.id === roleId)?.name ?? roleNotFound,
  );

  return defaultRolesNames.join(', ');
};

type Props = {
  authenticationBackend: DirectoryServiceBackend;
  excludedFields?: { [inputName: string]: boolean };
  roles: Immutable.List<Role>;
};

const UserSyncSection = ({ authenticationBackend, roles, excludedFields = {} }: Props) => {
  const { t } = useTranslation('authentication');
  const {
    userSearchBase,
    userSearchPattern,
    userNameAttribute,
    userFullNameAttribute,
    userUniqueIdAttribute,
    emailAttributes,
  } = authenticationBackend.config;
  const { defaultRoles = Immutable.List(), defaultUserTimezone } = authenticationBackend;

  return (
    <SectionComponent
      title={t('user_sync.title')}
      headerActions={<EditLinkButton authenticationBackendId={authenticationBackend.id} stepKey={USER_SYNC_KEY} />}>
      <ReadOnlyFormGroup label={t('user_sync.search_base_dn')} value={userSearchBase} />
      <ReadOnlyFormGroup label={t('user_sync.search_pattern')} value={userSearchPattern} />
      <ReadOnlyFormGroup label={t('user_sync.name_attribute')} value={userNameAttribute} />
      <ReadOnlyFormGroup label={t('user_sync.full_name_attribute')} value={userFullNameAttribute} />
      {!excludedFields.emailAttributes && (
        <ReadOnlyFormGroup label={t('user_sync.email_attribute')} value={emailAttributes.join(',')} />
      )}
      {!excludedFields.userUniqueIdAttribute && (
        <ReadOnlyFormGroup label={t('user_sync.id_attribute')} value={userUniqueIdAttribute} />
      )}
      <ReadOnlyFormGroup
        label={t('user_sync.default_roles')}
        value={rolesList(defaultRoles, roles, t('user_sync.role_not_found'))}
      />
      <ReadOnlyFormGroup
        label={t('user_sync.default_user_time_zone')}
        value={defaultUserTimezone || t('user_sync.browser_time_zone')}
      />
    </SectionComponent>
  );
};

export default UserSyncSection;
