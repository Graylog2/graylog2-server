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
import { MISSING_BUCKET_NAME } from 'views/Constants';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import { addToQuery, concatQueryStrings, not, predicate } from 'views/logic/queries/QueryHelper';
import recordQueryStringUsage from 'views/logic/queries/recordQueryStringUsage';
import { updateQueryString } from 'views/logic/slices/viewSlice';
import { selectQueryString } from 'views/logic/slices/viewSelectors';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import type { RootState } from 'views/types';
import { formatQueryValue, orValuesClause } from 'views/logic/valueactions/ValueActionQueryHelper';
import type { QueryValue } from 'views/logic/valueactions/ValueActionQueryHelper';

type Arguments = {
  queryId: string;
  field: string;
  values: Array<QueryValue>;
  type: FieldType;
};

// Matches any of the given values of a single field, e.g. `source:(a OR b)`.
// The missing bucket is matched with `NOT _exists_:field`, e.g. `(source:(a OR b) OR NOT _exists_:source)`.
export const anyValueClause = (field: string, values: Array<QueryValue>, type: FieldType) => {
  const presentValues = values.filter((value) => value !== MISSING_BUCKET_NAME);
  const includesMissingBucket = presentValues.length !== values.length;
  const presentValuesClause =
    presentValues.length === 1
      ? predicate(field, formatQueryValue(presentValues[0], type))
      : predicate(field, orValuesClause(presentValues.map((value) => ({ value, type }))));
  const clauses = [
    ...(presentValues.length > 0 ? [presentValuesClause] : []),
    ...(includesMissingBucket ? [predicate(field, MISSING_BUCKET_NAME)] : []),
  ];
  const clause = concatQueryStrings(clauses, { operator: 'OR', withBrackets: false });

  return clauses.length > 1 ? `(${clause})` : clause;
};

const updateQuery =
  (queryId: string, createNewQuery: (oldQuery: string) => string) =>
  async (dispatch: ViewsDispatch, getState: () => RootState) => {
    const oldQuery = selectQueryString(queryId)(getState());
    const newQuery = createNewQuery(oldQuery);

    await recordQueryStringUsage(newQuery, oldQuery);

    return dispatch(updateQueryString(queryId, newQuery));
  };

export const addValuesToQuery = ({ queryId, field, values, type }: Arguments) =>
  updateQuery(queryId, (oldQuery) => addToQuery(oldQuery, anyValueClause(field, values, type)));

export const excludeValuesFromQuery = ({ queryId, field, values, type }: Arguments) =>
  updateQuery(queryId, (oldQuery) => addToQuery(oldQuery, not(anyValueClause(field, values, type))));
