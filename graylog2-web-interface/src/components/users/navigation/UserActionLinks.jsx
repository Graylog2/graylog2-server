// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import User from 'logic/users/User';
import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/graylog';

type Props = {
  username: $PropertyType<User, 'username'>,
  userIsReadOnly: boolean,
};

const UserActionLinks = ({ username, userIsReadOnly }: Props) => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.USERS.show(username)}>
      <Button bsStyle="success">
        View Details
      </Button>
    </LinkContainer>
    {!userIsReadOnly && (
      <LinkContainer to={Routes.SYSTEM.USERS.edit(username)}>
        <Button bsStyle="success">
          Edit User
        </Button>
      </LinkContainer>
    )}
    <LinkContainer to={Routes.SYSTEM.USERS.TOKENS.edit(username)}>
      <Button bsStyle="success">
        Edit Tokens
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);

export default UserActionLinks;
