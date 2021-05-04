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

import { ReadOnlyFormGroup } from 'components/common';
import User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

import LoggedInIcon from '../LoggedInIcon';

type Props = {
  user: User,
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
  },
}: Props) => {
  const isOldUser = () => {
    return fullName && (!firstName && !lastName);
  };

  return (
    <SectionComponent title="Profile">
      <ReadOnlyFormGroup label="Username" value={username} />
      {isOldUser() && <ReadOnlyFormGroup label="Full name" value={fullName} />}
      <ReadOnlyFormGroup label="First Name" value={firstName} />
      <ReadOnlyFormGroup label="Last Name" value={lastName} />
      <ReadOnlyFormGroup label="E-Mail Address" value={email} />
      <ReadOnlyFormGroup label="Client Address" value={clientAddress} />
      <ReadOnlyFormGroup label="Last Activity" value={lastActivity} />
      <ReadOnlyFormGroup label="Logged In" value={<LoggedInIcon active={sessionActive} />} />
      <ReadOnlyFormGroup label="Enabled" value={accountStatus === 'enabled'} />
    </SectionComponent>
  );
};

export default ProfileSection;
