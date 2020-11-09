// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

import { Link } from 'components/graylog/router';
import Routes from 'routing/Routes';
import { IfPermitted, CountBadge } from 'components/common';
import type { UserContext } from 'logic/roles/Role';

type Props = {
  users: Immutable.Set<UserContext>,
};

const MAX_USERS = 10;

const UsersCell = ({ users = Immutable.Set() }: Props) => {
  const usersLength = users.size;
  const usersComponents = users.take(MAX_USERS).toArray().map(({ id, username }, index) => {
    return (
      <IfPermitted permissions={[`users:read:${username}`]} key={id}>
        <Link to={Routes.SYSTEM.USERS.show(id)}>{username}</Link>{index < (usersLength - 1) && ',  '}
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
