// @flow strict
import * as React from 'react';
import { Link } from 'react-router';

import Routes from 'routing/Routes';
import UserOverview from 'logic/users/UserOverview';

import ActionsCell from './ActionsCell';
import LoggedInCell from './LoggedInCell';
import RolesCell from './RolesCell';

type Props = {
  user: UserOverview,
  isActive: boolean,
};

const UsersOverviewItem = ({
  user: {
    clientAddress,
    email,
    fullName,
    lastActivity,
    sessionActive,
    username,
    readOnly,
    roles,
  },
  isActive,
}: Props) => {
  return (
    <tr key={username} className={isActive ? 'active' : ''}>
      <LoggedInCell lastActivity={lastActivity}
                    sessionActive={sessionActive} />
      <td className="limited">
        <Link to={Routes.SYSTEM.USERS.show(username)}>
          {fullName}
        </Link>
      </td>
      <td className="limited">{username}</td>
      <td className="limited">{email}</td>
      <td className="limited">{clientAddress}</td>
      <RolesCell roles={roles} />
      <ActionsCell username={username} readOnly={readOnly} />
    </tr>
  );
};

export default UsersOverviewItem;
