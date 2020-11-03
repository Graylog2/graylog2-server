// @flow strict
import * as React from 'react';

import { Link } from 'components/graylog/router';
import Routes from 'routing/Routes';
import UserOverview from 'logic/users/UserOverview';
import RolesCell from 'components/permissions/RolesCell';

import ActionsCell from './ActionsCell';
import LoggedInCell from './LoggedInCell';

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
      <RolesCell roles={roles} />
      <ActionsCell user={user} />
    </tr>
  );
};

export default UsersOverviewItem;
