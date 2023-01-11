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
import { createSelector } from '@reduxjs/toolkit';

import { DATE_TIME_FORMATS } from 'util/DateTime';
import { MISSING_BUCKET_NAME } from 'views/Constants';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';
import { selectSearch, selectViewType, setQueryString } from 'views/logic/slices/viewSlice';
import View from 'views/logic/views/View';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { RootState } from 'views/types';
import { setGlobalOverrideQuery, selectGlobalOverride } from 'views/logic/slices/searchExecutionSlice';
import type { ActionHandlerArguments } from 'views/components/actions/ActionHandler';

const formatTimestampForES = (value: string) => {
  const utc = moment(value).tz('UTC');

  return `"${utc.format(DATE_TIME_FORMATS.internalIndexer)}"`;
};

const formatNewQuery = (oldQuery: string, field: string, value: string, type: FieldType) => {
  const predicateValue = type.type === 'date'
    ? formatTimestampForES(value)
    : escape(value);
  const fieldPredicate = value === MISSING_BUCKET_NAME
    ? `NOT _exists_:${field}`
    : `${field}:${predicateValue}`;

  return addToQuery(oldQuery, fieldPredicate);
};

const selectCurrentQueryString = (queryId: string) => createSelector(
  selectViewType,
  selectGlobalOverride,
  selectSearch,
  (viewType, globalOverride, search) => (viewType === View.Type.Search
    ? search.queries.find((q) => q.id === queryId).query.query_string
    : globalOverride.query.query_string),
);

const AddToQueryHandler = ({ queryId, field, value = '', type }: ActionHandlerArguments<{}>) => (dispatch: AppDispatch, getState: () => RootState) => {
  const oldQuery = selectCurrentQueryString(queryId)(getState());
  const viewType = selectViewType(getState());
  const newQuery = formatNewQuery(oldQuery, field, value, type);

  if (viewType === View.Type.Search) {
    return dispatch(setQueryString(queryId, newQuery));
  }

  return dispatch(setGlobalOverrideQuery(newQuery));
};

export default AddToQueryHandler;
