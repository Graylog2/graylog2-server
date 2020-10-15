// @flow strict
import * as React from 'react';
import { LinkContainer } from 'components/graylog/router';

import { IfPermitted } from 'components/common';
import User from 'logic/users/User';
import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/graylog';

type Props = {
  username: $PropertyType<User, 'username'>,
  userIsReadOnly: boolean,
};

const UserManagementLinks = ({ username, userIsReadOnly }: Props) => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.USERS.show(username)}>
      <Button bsStyle="success">
        View Details
      </Button>
    </LinkContainer>
    {!userIsReadOnly && (
      <IfPermitted permissions="users:edit">
        <LinkContainer to={Routes.SYSTEM.USERS.edit(username)}>
          <Button bsStyle="success">
            Edit User
          </Button>
        </LinkContainer>
      </IfPermitted>
    )}
    <IfPermitted permissions="users:tokenlist">
      <LinkContainer to={Routes.SYSTEM.USERS.TOKENS.edit(username)}>
        <Button bsStyle="success">
          Edit Tokens
        </Button>
      </LinkContainer>
    </IfPermitted>
    <IfPermitted permissions="users:list">
      <LinkContainer to={Routes.SYSTEM.USERS.OVERVIEW}>
        <Button bsStyle="info">
          Users Overview
        </Button>
      </LinkContainer>
    </IfPermitted>
  </ButtonToolbar>
);

export default UserManagementLinks;
