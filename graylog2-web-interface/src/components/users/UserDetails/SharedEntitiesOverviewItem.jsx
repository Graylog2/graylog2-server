// @flow strict
import * as React from 'react';
import { Link } from 'react-router';

import type { GranteesList } from 'logic/permissions/EntityShareState';
import Routes from 'routing/Routes';
import SharedEntity from 'logic/permissions/SharedEntity';

type Props = {
  sharedEntity: SharedEntity,
};

const _getOwnerLink = ({ type, title }) => {
  switch (type) {
    case 'user':
      return Routes.SYSTEM.USERS.show(title);
    case 'team':
      return Routes.SYSTEM.USERS.show(title); // need to be updated when page exists
    default:
      throw new Error(`Owner of entity has not supported type: ${type}`);
  }
};

const OwnersCell = ({ owners }: {owners: GranteesList}) => (
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

const SharedEntitiesOverviewItem = ({
  sharedEntity: {
    owners,
    title,
    type,
  },
}: Props) => {
  return (
    <tr key={title + type}>
      <td className="limited">{title}</td>
      <td className="limited">{type}</td>
      <OwnersCell owners={owners} />
      <td className="limited">Viewer</td>
    </tr>
  );
};

export default SharedEntitiesOverviewItem;
