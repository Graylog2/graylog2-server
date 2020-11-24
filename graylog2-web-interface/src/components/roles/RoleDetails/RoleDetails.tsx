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

import { Spinner } from 'components/common';
import SectionGrid from 'components/common/Section/SectionGrid';
import Role from 'logic/roles/Role';

import TeamsSection from './TeamsSection';
import ProfileSection from './ProfileSection';
import UsersSection from './UsersSection';

type Props = {
  role: Role | null | undefined,
};

const RoleDetails = ({ role }: Props) => {
  if (!role) {
    return <Spinner />;
  }

  return (
    <SectionGrid>
      <div>
        <ProfileSection role={role} />
      </div>
      <div>
        <UsersSection role={role} />
        <TeamsSection role={role} />
      </div>
    </SectionGrid>
  );
};

export default RoleDetails;
