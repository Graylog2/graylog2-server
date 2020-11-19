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

import { Link } from 'components/graylog/router';
import Routes from 'routing/Routes';
import UserOverview from 'logic/users/UserOverview';
import RolesCell from 'components/permissions/RolesCell';

import ActionsCell from './ActionsCell';
import LoggedInCell from './LoggedInCell';
import StatusCell from './StatusCell';

type Props = {
  user: UserOverview,
  isActive: boolean,
};

const UsersOverviewItem = ({
  user,
  user: {
    id,
    clientAddress,
    email,
    fullName,
    lastActivity,
    sessionActive,
    username,
    roles,
    accountStatus,
  },
  isActive,
}: Props) => {
  return (
    <tr key={username} className={isActive ? 'active' : ''}>
      <LoggedInCell lastActivity={lastActivity}
                    sessionActive={sessionActive}
                    clientAddress={clientAddress} />
      <td className="limited">
        <Link to={Routes.SYSTEM.USERS.show(id)}>
          {fullName}
        </Link>
      </td>
      <td className="limited">{username}</td>
      <td className="limited">{email}</td>
      <td className="limited">{clientAddress}</td>
      <StatusCell accountStatus={accountStatus} />
      <RolesCell roles={roles} />
      <ActionsCell user={user} />
    </tr>
  );
};

export default UsersOverviewItem;
