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
import moment from 'moment-timezone';

import type FieldType from 'views/logic/fieldtypes/FieldType';
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';
import type { ActionHandler } from 'views/components/actions/ActionHandler';
import { DATE_TIME_FORMATS } from 'util/DateTime';

import QueryManipulationHandler from './QueryManipulationHandler';

const formatTimestampForES = (value: string) => {
  const utc = moment(value).tz('UTC');

  return `"${utc.format(DATE_TIME_FORMATS.internalIndexer)}"`;
};

const formatNewQuery = (oldQuery: string, field: string, value: string, type: FieldType) => {
  const predicateValue = type.type === 'date'
    ? formatTimestampForES(value)
    : escape(value);
  const fieldPredicate = value === '(Empty Value)'
    ? `NOT _exists_:${field}`
    : `${field}:${predicateValue}`;

  return addToQuery(oldQuery, fieldPredicate);
};

export default class AddToQueryHandler extends QueryManipulationHandler {
  handle: ActionHandler<{}> = ({ queryId, field, value = '', type }) => {
    const oldQuery = this.currentQueryString(queryId);
    const newQuery = formatNewQuery(oldQuery, field, value, type);

    return this.updateQueryString(queryId, newQuery);
  };
}
