// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import { Link } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';

import Role from 'logic/roles/Role';
import Routes from 'routing/Routes';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import UserOverview from 'logic/users/UserOverview';
import { Button, ButtonToolbar } from 'components/graylog';
import RolesCell from 'components/users/UsersOverview/UserOverviewItem/RolesCell';

type Props = {
  user: UserOverview,
  roles: Immutable.List<Role>,
};

const ActtionsWrapper = styled(ButtonToolbar)`
  display: flex;
  justify-content: flex-end;
`;

const SyncedUsersOverviewItem = ({
  user: {
    enabled,
    fullName,
    id,
    roles: userRolesIds,
    username,
  },
  roles,
}: Props) => {
  const userRolesNames = userRolesIds.map((roleId) => {
    return roles.find((role) => role.id === roleId)?.name;
  });

  return (
    <tr key={username}>
      <td className="limited">
        <Link to={Routes.SYSTEM.USERS.show(username)}>
          {username}
        </Link>
      </td>
      <td className="limited">{fullName}</td>
      <RolesCell roles={userRolesNames} />
      <td className="limited">
        <ActtionsWrapper>
          {enabled
            ? (
              <Button type="button" bsStyle="info" bsSize="xs" onClick={() => AuthenticationDomain.disableUser(id, username)}>
                Disable
              </Button>
            ) : (
              <Button type="button" bsStyle="info" bsSize="xs" onClick={() => AuthenticationDomain.enableUser(id, username)}>
                Enable
              </Button>
            )}
          <LinkContainer to={Routes.SYSTEM.USERS.edit(encodeURIComponent(username))}>
            <Button type="button" bsStyle="info" bsSize="xs">
              Edit
            </Button>
          </LinkContainer>
        </ActtionsWrapper>
      </td>
    </tr>
  );
};

export default SyncedUsersOverviewItem;
