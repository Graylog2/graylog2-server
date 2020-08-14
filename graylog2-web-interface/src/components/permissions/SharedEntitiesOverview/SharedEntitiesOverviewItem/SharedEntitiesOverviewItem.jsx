// @flow strict
import * as React from 'react';
import { Link } from 'react-router';

import SharedEntity from 'logic/permissions/SharedEntity';
import { getShowRouteFromGRN } from 'logic/permissions/GRN';

import OwnersCell from './OwnersCell';

type Props = {
  capabilityTitle: string,
  sharedEntity: SharedEntity,
};

const SharedEntitiesOverviewItem = ({
  capabilityTitle,
  sharedEntity: {
    owners,
    title,
    type,
    id,
  },
}: Props) => (
  <tr key={title + type}>
    <td className="limited">
      <Link to={getShowRouteFromGRN(id)}>{title}</Link>
    </td>
    <td className="limited">{type}</td>
    <OwnersCell owners={owners} />
    <td className="limited">{capabilityTitle}</td>
  </tr>
);

export default SharedEntitiesOverviewItem;
