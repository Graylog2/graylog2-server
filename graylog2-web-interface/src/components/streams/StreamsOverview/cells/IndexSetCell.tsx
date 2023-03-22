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
import * as React from 'react';

import type { Stream } from 'stores/streams/StreamsStore';
import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

type Props = {
  stream: Stream,
  indexSets: Array<IndexSet>
};

const IndexSetCell = ({ stream, indexSets }: Props) => {
  const currentUser = useCurrentUser();

  if (!isPermitted(currentUser.permissions, ['indexsets:read'])) {
    return null;
  }

  const indexSet = indexSets.find((is) => is.id === stream.index_set_id) || indexSets.find((is) => is.default);

  return (
    indexSet ? (
      <Link to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id)}>
        {indexSet.title}
      </Link>
    ) : <i>not found</i>
  );
};

export default IndexSetCell;
