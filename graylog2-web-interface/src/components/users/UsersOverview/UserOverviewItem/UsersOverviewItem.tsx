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
import styled from 'styled-components';

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import { createGRN } from 'logic/permissions/GRN';
import useHasEntityPermissionByGRN from 'hooks/useHasEntityPermissionByGRN';
import type UserOverview from 'logic/users/UserOverview';
import { RestrictedAccessTooltip } from 'components/common';
import RolesCell from 'components/permissions/RolesCell';

import ActionsCell from './ActionsCell';
import LoggedInCell from './LoggedInCell';
import StatusCell from './StatusCell';

type Props = {
  user: UserOverview;
  isActive: boolean;
};

const NameColumnWrapper = styled.div`
  display: flex;
  align-items: center;
`;

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
    authServiceEnabled,
  },
  isActive,
}: Props) => {
  const grn = createGRN('user', username);
  const hasEditPermissions = useHasEntityPermissionByGRN(grn, 'edit');

  return (
    <tr key={username} className={isActive ? 'active' : ''}>
      <LoggedInCell lastActivity={lastActivity} sessionActive={sessionActive} clientAddress={clientAddress} />
      <td className="limited">
        <NameColumnWrapper>
          {hasEditPermissions ? (
            <Link to={Routes.SYSTEM.USERS.show(id)}>{fullName}</Link>
          ) : (
            <>
              {fullName}
              <RestrictedAccessTooltip entityName="user" capabilityName="view" />
            </>
          )}
        </NameColumnWrapper>
      </td>
      <td className="limited">{username}</td>
      <td className="limited">{email}</td>
      <td className="limited">{clientAddress}</td>
      <StatusCell accountStatus={accountStatus} authServiceEnabled={authServiceEnabled} />
      <RolesCell roles={roles} />
      <ActionsCell user={user} />
    </tr>
  );
};

export default UsersOverviewItem;
