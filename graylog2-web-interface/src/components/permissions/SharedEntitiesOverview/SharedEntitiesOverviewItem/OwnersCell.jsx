// @flow strict
import * as React from 'react';
import { Link } from 'react-router';

import Routes from 'routing/Routes';
import type { GranteesList } from 'logic/permissions/EntityShareState';
import { getIdFromGRN } from 'logic/permissions/GRN';

type Props = {
  owners: GranteesList,
};

const _getOwnerLink = ({ type, id }) => {
  const ownerId = getIdFromGRN(id, type);

  switch (type) {
    case 'user':
      return Routes.SYSTEM.USERS.show(ownerId);
    case 'team':
      return Routes.pluginRoute('SYSTEM_TEAMS_TEAMID')(ownerId);
    default:
      throw new Error(`Owner of entity has not supported type: ${type}`);
  }
};

const OwnersCell = ({ owners }: Props) => (
  <td className="limited">
    {owners.map((owner, index) => {
      const link = _getOwnerLink(owner);
      const isLast = index >= owners.size - 1;

      return (
        <React.Fragment key={owner.id}>
          <Link to={link}>{owner.title}</Link>
          {!isLast && ', '}
        </React.Fragment>
      );
    })}
  </td>
);

export default OwnersCell;
