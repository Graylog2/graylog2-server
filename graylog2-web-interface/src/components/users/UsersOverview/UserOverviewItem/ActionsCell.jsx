// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';
import { LinkContainer } from 'react-router-bootstrap';

import type { ThemeInterface } from 'theme';
import User from 'logic/users/User';
import UsersActions from 'actions/users/UsersActions';
import Routes from 'routing/Routes';
import { Button, OverlayTrigger, Tooltip, DropdownButton, MenuItem } from 'components/graylog';
import { IfPermitted } from 'components/common';

type Props = {
  readOnly: boolean,
  username: $PropertyType<User, 'username'>,
};

const Td: StyledComponent<{}, ThemeInterface, HTMLTableCellElement> = styled.td`
  width: 180px;
`;

const EditTokensAction = ({ username }: { username: $PropertyType<Props, 'username'> }) => (
  <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.USERS.TOKENS.edit(encodeURIComponent(username))}>
    <Button id={`edit-tokens-${username}`} bsStyle="info" bsSize="xs" title={`Edit tokens of user ${username}`}>
      Edit tokens
    </Button>
  </LinkContainer>
);

const ReadOnlyActions = ({ username }: { username: $PropertyType<Props, 'username'> }) => {
  const tooltip = <Tooltip id="system-user">System users can only be modified in the Graylog configuration file.</Tooltip>;

  // Todo: Add eventKey 1 for MenuItem + curso:help for System user parent span
  return (
    <span>
      <OverlayTrigger placement="left" overlay={tooltip}>
        <span>
          <Button bsSize="xs" bsStyle="info" disabled>System user</Button>
        </span>
      </OverlayTrigger>
      &nbsp;
      <EditTokensAction username={username} />
    </span>
  );
};

const EditActions = ({ username }: { username: $PropertyType<Props, 'username'> }) => {
  const _deleteUser = () => {
    if (window.confirm(`Do you really want to delete user ${username}?`)) {
      UsersActions.deleteUser(username);
    }
  };

  return (
    <div>
      <IfPermitted permissions={[`users:edit:${username}`]}>
        <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.USERS.edit(encodeURIComponent(username))}>
          <Button id={`edit-user-${username}`} bsStyle="info" bsSize="xs" title={`Edit user ${username}`}>
            Edit
          </Button>
        </LinkContainer>
      </IfPermitted>
      &nbsp;
      <DropdownButton bsSize="xs" title="More actions" pullRight>
        <EditTokensAction username={username} />
        <MenuItem eventKey="2"
                  id={`delete-user-${username}`}
                  bsStyle="primary"
                  bsSize="xs"
                  title="Delete user"
                  onClick={_deleteUser}>
          Delete
        </MenuItem>
      </DropdownButton>
    </div>
  );
};

const UserActionsCol = ({ username, readOnly }: Props) => {
  return (
    <Td>
      {readOnly ? (
        <ReadOnlyActions username={username} />
      ) : (
        <EditActions username={username} />
      )}
    </Td>
  );
};

export default UserActionsCol;
