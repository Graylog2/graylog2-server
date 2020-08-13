// @flow strict
import * as React from 'react';
import { Link } from 'react-router';

import { getIdFromGRN } from 'logic/permissions/GRN';
import Routes from 'routing/Routes';
import SharedEntity from 'logic/permissions/SharedEntity';

type Props = {
  title: $PropertyType<SharedEntity, 'title'>,
  type: $PropertyType<SharedEntity, 'type'>,
  id: $PropertyType<SharedEntity, 'id'>,
};

const _getTitleLink = (id, type) => {
  const entityId = getIdFromGRN(id, type);

  switch (type) {
    case 'dashboard':
      return Routes.pluginRoute('DASHBOARDS_VIEWID')(entityId);
    case 'search':
      return Routes.pluginRoute('SEARCH_VIEWID')(entityId);
    case 'stream':
      return Routes.stream_search(entityId);
    default:
      throw new Error(`Owner of entity has not supported type: ${type}`);
  }
};

const TitleCell = ({ title, type, id }: Props) => {
  const link = _getTitleLink(id, type);

  return (
    <td className="limited">
      <Link to={link}>{title}</Link>
    </td>
  );
};

export default TitleCell;
