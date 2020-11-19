/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
