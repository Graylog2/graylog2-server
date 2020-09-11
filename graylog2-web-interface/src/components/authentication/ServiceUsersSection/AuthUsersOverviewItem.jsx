// @flow strict
import * as React from 'react';
import { Link } from 'react-router';
import styled from 'styled-components';

import RolesCell from 'components/users/UsersOverview/UserOverviewItem/RolesCell';
import { Button, ButtonToolbar } from 'components/graylog';
import Routes from 'routing/Routes';
import AuthenticationUser from 'logic/authentication/AuthenticationUser';

type Props = {
  user: AuthenticationUser,
};

const ActtionsWrapper = styled(ButtonToolbar)`
  display: flex;
  justify-content: flex-end;
`;

const AuthUsersOverviewItem = ({
  user: {
    fullName,
    username,
    roles,
    enabled,
  },
}: Props) => {
  return (
    <tr key={username}>
      <td className="limited">
        <Link to={Routes.SYSTEM.USERS.show(username)}>
          {username}
        </Link>
      </td>
      <td className="limited">{fullName}</td>
      <RolesCell roles={roles} />
      <td className="limited">
        <ActtionsWrapper>
          {enabled
            ? (
              <Button type="button" bsStyle="info" bsSize="xs" onClick={() => {}}>
                Disable
              </Button>
            ) : (
              <Button type="button" bsStyle="info" bsSize="xs" onClick={() => {}}>
                Enable
              </Button>
            )}
          <Button type="button" bsStyle="danger" bsSize="xs" onClick={() => {}}>
            Delete
          </Button>
        </ActtionsWrapper>
      </td>
    </tr>
  );
};

export default AuthUsersOverviewItem;
