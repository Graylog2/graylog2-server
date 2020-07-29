// @flow strict
import * as React from 'react';

import SharedEntity from 'logic/permissions/SharedEntity';

type Props = {
  sharedEntity: SharedEntity,
};

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
      <td className="limited">{owners.toJS().join(', ')}</td>
      <td className="limited">Viewer</td>
    </tr>
  );
};

export default SharedEntitiesOverviewItem;
