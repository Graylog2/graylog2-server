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

import { Link } from 'components/graylog/router';
import Routes from 'routing/Routes';
import { IfPermitted, CountBadge } from 'components/common';
import type { UserContext } from 'actions/roles/AuthzRolesActions';

type Props = {
  users: Immutable.Set<UserContext>,
};

const MAX_USERS = 10;

const UsersCell = ({ users = Immutable.Set() }: Props) => {
  const usersLength = users.size;
  const usersComponents = users.take(MAX_USERS).toArray().map(({ id, username }, index) => {
    return (
      <IfPermitted permissions={[`users:read:${username}`]} key={id}>
        <>
          <Link to={Routes.SYSTEM.USERS.show(id)}>{username}</Link>{index < (usersLength - 1) && ',  '}
        </>
      </IfPermitted>
    );
  });

  if (usersLength > MAX_USERS) {
    usersComponents.push(<span key="dots">...</span>);
  }

  return (
    <td>
      <CountBadge>{users.size}</CountBadge>
      {' '}
      {usersComponents}
    </td>
  );
};

export default UsersCell;
