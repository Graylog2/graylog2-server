// @flow strict
import * as React from 'react';

import { LinkContainer } from 'components/graylog/router';
import User from 'logic/users/User';
import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/graylog';

type Props = {
  userId: $PropertyType<User, 'id'>,
  userIsReadOnly: boolean,
};

const UserActionLinks = ({ userId, userIsReadOnly }: Props) => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.USERS.show(userId)}>
      <Button bsStyle="success">
        View Details
      </Button>
    </LinkContainer>
    {!userIsReadOnly && (
      <LinkContainer to={Routes.SYSTEM.USERS.edit(userId)}>
        <Button bsStyle="success">
          Edit User
        </Button>
      </LinkContainer>
    )}
    <LinkContainer to={Routes.SYSTEM.USERS.TOKENS.edit(userId)}>
      <Button bsStyle="success">
        Edit Tokens
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);

export default UserActionLinks;
