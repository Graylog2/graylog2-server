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
import styled from 'styled-components';

import { LinkContainer, Link } from 'components/graylog/router';
import Role from 'logic/roles/Role';
import Routes from 'routing/Routes';
import UserOverview from 'logic/users/UserOverview';
import { Button, ButtonToolbar } from 'components/graylog';
import RolesCell from 'components/permissions/RolesCell';

type Props = {
  user: UserOverview,
  roles: Immutable.List<Role>,
};

const ActionsWrapper = styled(ButtonToolbar)`
  display: flex;
  justify-content: flex-end;
`;

const SyncedUsersOverviewItem = ({
  user: {
    fullName,
    id,
    roles: userRolesIds,
    username,
  },
  roles,
}: Props) => {
  const userRolesNames = userRolesIds.map((roleId) => {
    return roles.find((role) => role.id === roleId)?.name ?? 'Role not found';
  }).toSet();

  return (
    <tr key={id}>
      <td className="limited">
        <Link to={Routes.SYSTEM.USERS.show(id)}>
          {username}
        </Link>
      </td>
      <td className="limited">{fullName}</td>
      <RolesCell roles={userRolesNames} />
      <td className="limited">
        <ActionsWrapper>
          <LinkContainer to={Routes.SYSTEM.USERS.edit(id)}>
            <Button type="button" bsStyle="info" bsSize="xs">
              Edit
            </Button>
          </LinkContainer>
        </ActionsWrapper>
      </td>
    </tr>
  );
};

export default SyncedUsersOverviewItem;
