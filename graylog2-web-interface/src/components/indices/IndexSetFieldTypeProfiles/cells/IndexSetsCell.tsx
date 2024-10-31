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
import { styled } from 'styled-components';

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';

const List = styled.div`
  display: flex;
  flex-wrap: wrap;
`;

const IndexSetsCell = ({ indexSetIds, normalizedIndexSetsTitles }: { indexSetIds : Array<string>, normalizedIndexSetsTitles: Record<string, string> }) => {
  const currentUser = useCurrentUser();

  if (!isPermitted(currentUser.permissions, ['indexsets:read'])) {
    return null;
  }

  return (
    <List>
      {indexSetIds
        .map((indexSetId, index) => {
          const isLast = index === indexSetIds.length - 1;

          return (
            <>
              <Link key={indexSetId}
                    to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSetId)}
                    target="_blank">
                {normalizedIndexSetsTitles[indexSetId]}
              </Link>
              {!isLast && ', '}
            </>
          );
        },
        )}
    </List>
  );
};

export default IndexSetsCell;
