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
import uniq from 'lodash/uniq';

import type FieldType from 'views/logic/fieldtypes/FieldType';
import recordQueryStringUsage from 'views/logic/queries/recordQueryStringUsage';
import {
  escape,
  addToQuery,
  formatTimestamp,
  predicate,
  concatQueryStrings,
  edgeClause,
} from 'views/logic/queries/QueryHelper';
import { updateQueryString } from 'views/logic/slices/viewSlice';
import { selectQueryString } from 'views/logic/slices/viewSelectors';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import type { RootState, ActionContexts } from 'views/types';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import hasMultipleValueForActions from 'views/components/visualizations/utils/hasMultipleValueForActions';

const toPredicate = (field: string, value: string | number, type: FieldType) =>
  predicate(field, type.type === 'date' ? formatTimestamp(value) : escape(value));

const formatNewQuery = (oldQuery: string, field: string, value: string | number, type: FieldType) =>
  addToQuery(oldQuery, toPredicate(field, value, type));

type QueryValue = string | number;
type ValueToAdd = { field: string; value: QueryValue; type: FieldType };
type ValueArgument = QueryValue | Array<QueryValue>;

// Joins predicates with OR into a single bracketed clause, e.g. `(source:a OR target:a)`.
const orClause = (values: Array<ValueToAdd>) =>
  `(${concatQueryStrings(
    values.map(({ field, value, type }) => toPredicate(field, value, type)),
    { operator: 'OR', withBrackets: false },
  )})`;

type Arguments = {
  queryId: string;
  field: string;
  value?: ValueArgument;
  type: FieldType;
  contexts?: Partial<ActionContexts>;
};

const valuesFromPath = (contexts: Partial<ActionContexts>) => contexts.valuePath.map((path) => {
  const [pathField, pathValue] = Object.entries(path)[0];

  return { field: pathField, value: pathValue, type: fieldTypeFor(pathField, contexts?.fieldTypes) };
})

const valuesFromArray = (field: string, value: Array<QueryValue>, type: FieldType): Array<ValueToAdd> =>
  value.map((arrayValue) => ({ field, value: arrayValue, type }))

const AddToQueryHandler =
  ({ queryId, field, value = '', type, contexts }: Arguments) =>
  async (dispatch: ViewsDispatch, getState: () => RootState) => {
    const oldQuery = selectQueryString(queryId)(getState());
    const hasMultipleValuesInPath = hasMultipleValueForActions(contexts);
    const fieldValueIsArray = Array.isArray(value);

    const getValues = () => {
      if(hasMultipleValuesInPath) return valuesFromPath(contexts);
      if(fieldValueIsArray) return valuesFromArray(field, value, type);

      return [{ field, value, type }]
    }

    const valuesToAdd: Array<ValueToAdd> = uniq(getValues());

    let newQuery: string;

    const shouldAddWithOr = (hasMultipleValuesInPath && contexts?.valuePathOperator === 'OR')
      || (!hasMultipleValuesInPath && fieldValueIsArray && valuesToAdd.length > 1)

    if (hasMultipleValuesInPath && contexts?.valuePathOperator === 'EDGE') {
      newQuery = addToQuery(
        oldQuery,
        edgeClause(
          { field: valuesToAdd[0].field, value: valuesToAdd[0].value },
          { field: valuesToAdd[1].field, value: valuesToAdd[1].value },
        ),
      );
    } else if (shouldAddWithOr) {
      newQuery = addToQuery(oldQuery, orClause(valuesToAdd));
    } else {
      newQuery = valuesToAdd.reduce(
        (prev, valueToAdd) => formatNewQuery(prev, valueToAdd.field, valueToAdd.value, valueToAdd.type),
        oldQuery,
      );
    }

    await recordQueryStringUsage(newQuery, oldQuery);

    return dispatch(updateQueryString(queryId, newQuery));
  };

export default AddToQueryHandler;
