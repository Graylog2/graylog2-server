// @flow strict
import * as React from 'react';
import { Link } from 'react-router';

import Routes from 'routing/Routes';
import Role from 'logic/roles/Role';

import ActionsCell from './ActionsCell';

type Props = {
  role: Role,
};

const RolesOverviewItem = ({
  role: {
    id,
    name,
    description,
    readOnly,
  },
}: Props) => {
  return (
    <tr key={id}>
      <td className="limited">
        <Link to={Routes.SYSTEM.AUTHZROLES.show(id)}>
          {name}
        </Link>
      </td>
      <td className="limited">{description}</td>
      <ActionsCell roleId={id} roleName={name} readOnly={readOnly} />
    </tr>
  );
};

export default RolesOverviewItem;
