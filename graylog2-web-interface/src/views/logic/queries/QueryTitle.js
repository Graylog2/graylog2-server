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
// @flow strict
import type { QueryId } from './Query';

import View from '../views/View';
import ViewState from '../views/ViewState';

const queryTitle = (view: View, queryId: QueryId): ?string => (view && view.search && view.search.queries
  ? view.search.queries.keySeq()
    .map((q, idx) => {
      if (queryId !== undefined && q.id !== undefined && queryId === q.id) {
        return view.state
          ? view.state.getIn([q.id], ViewState.create()).titles.getIn(['tab', 'title'], `Page#${idx + 1}`)
          : `Page#${idx + 1}`;
      }

      return undefined;
    }).filter((title) => title !== undefined)
    .first()
  : undefined);

export default queryTitle;
