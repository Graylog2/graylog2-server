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
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';
import type { ActionHandler } from 'views/components/actions/ActionHandler';
import { MISSING_BUCKET_NAME } from 'views/Constants';

import QueryManipulationHandler from './QueryManipulationHandler';

export default class ExcludeFromQueryHandler extends QueryManipulationHandler {
  static formatNewQuery = (oldQuery: string, field: string, value: any) => {
    const fieldPredicate = value === MISSING_BUCKET_NAME
      ? `_exists_:${field}`
      : `NOT ${field}:${escape(value)}`;

    return addToQuery(oldQuery, fieldPredicate);
  };

  handle: ActionHandler<{}> = ({ queryId, field, value }) => {
    const oldQuery = this.currentQueryString(queryId);
    const newQuery = ExcludeFromQueryHandler.formatNewQuery(oldQuery, field, value);

    return this.updateQueryString(queryId, newQuery);
  };
}
