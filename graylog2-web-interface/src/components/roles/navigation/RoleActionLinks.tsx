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
import * as React from 'react';
import type { $PropertyType } from 'utility-types';

import { LinkContainer } from 'components/common/router';
import type Role from 'logic/roles/Role';
import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/bootstrap';

type Props = {
  roleId: $PropertyType<Role, 'id'>,
};

const RoleActionLinks = ({ roleId }: Props) => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.AUTHZROLES.show(roleId)}>
      <Button bsStyle="success">
        View Details
      </Button>
    </LinkContainer>
    <LinkContainer to={Routes.SYSTEM.AUTHZROLES.edit(roleId)}>
      <Button bsStyle="success">
        Edit Role
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);

export default RoleActionLinks;
