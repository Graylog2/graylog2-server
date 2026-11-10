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
import React from 'react';

import { Link } from 'components/common';
import ExpandableCellList from 'components/common/cell-list/ExpandableCellList';
import Routes from 'routing/Routes';
import usePermissions from 'hooks/usePermissions';

type Props = {
  indexSetIds: Array<string>;
  normalizedIndexSetsTitles: Record<string, string>;
};

const IndexSetsCell = ({ indexSetIds, normalizedIndexSetsTitles }: Props) => {
  const { isPermitted } = usePermissions();

  if (!isPermitted(['indexsets:read'])) {
    return null;
  }

  const title = (indexSetId: string) => normalizedIndexSetsTitles[indexSetId] ?? indexSetId;

  return (
    <ExpandableCellList
      items={[...indexSetIds].sort((a, b) => title(a).localeCompare(title(b)))}
      renderItem={(indexSetId) => (
        <Link to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSetId)} target="_blank">
          {title(indexSetId)}
        </Link>
      )}
      itemKey={(indexSetId) => indexSetId}
      itemTitle={title}
    />
  );
};

export default IndexSetsCell;
