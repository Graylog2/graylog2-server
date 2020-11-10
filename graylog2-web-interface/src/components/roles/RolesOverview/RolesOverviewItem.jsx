// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

import { Link } from 'components/graylog/router';
import Routes from 'routing/Routes';
import Role from 'logic/roles/Role';
import type { UserContext } from 'actions/roles/AuthzRolesActions';

import ActionsCell from './ActionsCell';
import UsersCell from './UsersCell';

type Props = {
  role: Role,
  users: UserContext[],
};

const RolesOverviewItem = ({
  role: {
    id,
    name,
    description,
    readOnly,
  },
  users,
}: Props) => {
  return (
    <tr key={id}>
      <td className="limited">
        <Link to={Routes.SYSTEM.AUTHZROLES.show(id)}>
          {name}
        </Link>
      </td>
      <td>{description}</td>
      <UsersCell users={Immutable.Set(users)} />
      <ActionsCell roleId={id} roleName={name} readOnly={readOnly} />
    </tr>
  );
};

export default RolesOverviewItem;
