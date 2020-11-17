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

import { Spinner, IfPermitted } from 'components/common';
import Role from 'logic/roles/Role';

import UsersSection from './UsersSection';
import TeamsSection from './TeamsSection';

import ProfileSection from '../RoleDetails/ProfileSection';
import SectionGrid from '../../common/Section/SectionGrid';

type Props = {
  role: ?Role,
};

const RoleEdit = ({ role }: Props) => {
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
        <IfPermitted permissions="teams:edit">
          <TeamsSection role={role} />
        </IfPermitted>
      </div>
    </SectionGrid>
  );
};

export default RoleEdit;
