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
import { useTranslation } from 'react-i18next';

import { ReadOnlyFormGroup } from 'components/common';
import type User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  user: User;
};

const ProfileSection = ({
  user: {
    username,
    fullName,
    firstName,
    lastName,
    email,
    clientAddress,
    lastActivity,
    sessionActive,
    accountStatus,
    authServiceEnabled,
  },
}: Props) => {
  const { t } = useTranslation('users');
  const isOldUser = () => fullName && !firstName && !lastName;

  return (
    <SectionComponent title={t('user_fields.profile')}>
      <ReadOnlyFormGroup label={t('user_fields.username')} value={username} />
      {isOldUser() && <ReadOnlyFormGroup label={t('user_fields.full_name')} value={fullName} />}
      <ReadOnlyFormGroup label={t('user_fields.first_name')} value={firstName} />
      <ReadOnlyFormGroup label={t('user_fields.last_name')} value={lastName} />
      <ReadOnlyFormGroup label={t('user_fields.email')} value={email} />
      <ReadOnlyFormGroup label={t('user_details.client_address')} value={clientAddress} />
      <ReadOnlyFormGroup label={t('user_details.last_activity')} value={lastActivity} />
      <ReadOnlyFormGroup label={t('user_details.logged_in')} value={sessionActive} />
      <ReadOnlyFormGroup
        label={t('user_details.enabled')}
        value={accountStatus === 'enabled'}
        help={!authServiceEnabled && accountStatus === 'enabled' ? t('user_details.auth_service_disabled') : ''}
      />
    </SectionComponent>
  );
};

export default ProfileSection;
