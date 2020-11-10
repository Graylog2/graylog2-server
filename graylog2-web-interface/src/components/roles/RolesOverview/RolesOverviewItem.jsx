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
