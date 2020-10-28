// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import { LinkContainer, Link } from 'components/graylog/router';
import Role from 'logic/roles/Role';
import Routes from 'routing/Routes';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
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
    enabled,
    fullName,
    id,
    roles: userRolesIds,
    username,
  },
  roles,
}: Props) => {
  const userRolesNames = userRolesIds.map((roleId) => {
    return roles.find((role) => role.id === roleId)?.name ?? 'Role not found';
  });

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
        </ActionsWrapper>
      </td>
    </tr>
  );
};

export default SyncedUsersOverviewItem;
