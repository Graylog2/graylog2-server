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
import * as Immutable from 'immutable';
import styled from 'styled-components';

import { Link } from 'components/graylog/router';
import Routes from 'routing/Routes';
import Role from 'logic/roles/Role';
import { TextOverflowEllipsis } from 'components/common';
import type { UserContext } from 'actions/roles/AuthzRolesActions';

import ActionsCell from './ActionsCell';
import UsersCell from './UsersCell';

const DescriptionCell = styled.td`
  max-width: 300px;
`;

type Props = {
  role: Role,
  users: UserContext[],
};

const RolesOverviewItem = ({
  role: {
    id,
    name,
    description,
    readOnly,
  },
  users,
}: Props) => {
  return (
    <tr key={id}>
      <td className="limited">
        <Link to={Routes.SYSTEM.AUTHZROLES.show(id)}>
          {name}
        </Link>
      </td>
      <DescriptionCell>
        <TextOverflowEllipsis>
          {description}
        </TextOverflowEllipsis>
      </DescriptionCell>
      <UsersCell users={Immutable.Set(users)} />
      <ActionsCell roleId={id} roleName={name} readOnly={readOnly} />
    </tr>
  );
};

export default RolesOverviewItem;
