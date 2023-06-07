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

import { DATE_TIME_FORMATS } from 'util/DateTime';
import { MISSING_BUCKET_NAME } from 'views/Constants';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';
import { updateQueryString } from 'views/logic/slices/viewSlice';
import { selectQueryString } from 'views/logic/slices/viewSelectors';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { RootState } from 'views/types';

const formatTimestampForES = (value: string | number) => {
  const utc = moment(value).tz('UTC');

  return `"${utc.format(DATE_TIME_FORMATS.internalIndexer)}"`;
};

const formatNewQuery = (oldQuery: string, field: string, value: string | number, type: FieldType) => {
  const predicateValue = type.type === 'date'
    ? formatTimestampForES(value)
    : escape(value);
  const fieldPredicate = value === MISSING_BUCKET_NAME
    ? `NOT _exists_:${field}`
    : `${field}:${predicateValue}`;

  return addToQuery(oldQuery, fieldPredicate);
};

type Arguments = {
  queryId: string,
  field: string,
  value?: string | number,
  type: FieldType,
};

const AddToQueryHandler = ({ queryId, field, value = '', type }: Arguments) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const oldQuery = selectQueryString(queryId)(getState());
  const newQuery = formatNewQuery(oldQuery, field, value, type);

  return dispatch(updateQueryString(queryId, newQuery));
};

export default AddToQueryHandler;
