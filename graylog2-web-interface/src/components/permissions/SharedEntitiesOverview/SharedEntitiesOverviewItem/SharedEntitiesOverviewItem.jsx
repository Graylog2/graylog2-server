// @flow strict
import * as React from 'react';

import SharedEntity from 'logic/permissions/SharedEntity';

import OwnersCell from './OwnersCell';
import TitleCell from './TitleCell';

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
    <TitleCell title={title} type={type} id={id} />
    <td className="limited">{type}</td>
    <OwnersCell owners={owners} />
    <td className="limited">{capabilityTitle}</td>
  </tr>
);

export default SharedEntitiesOverviewItem;
