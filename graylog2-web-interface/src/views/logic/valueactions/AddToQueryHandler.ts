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

type ValueToAdd = { field: string; value: string | number; type: FieldType };

// Joins each value path entry with OR into a single bracketed clause, e.g. `(source:a OR target:a)`.
const orClause = (values: Array<ValueToAdd>) =>
  `(${concatQueryStrings(
    values.map(({ field, value, type }) => toPredicate(field, value, type)),
    { operator: 'OR', withBrackets: false },
  )})`;

type Arguments = {
  queryId: string;
  field: string;
  value?: string | number;
  type: FieldType;
  contexts?: Partial<ActionContexts>;
};

const AddToQueryHandler =
  ({ queryId, field, value = '', type, contexts }: Arguments) =>
  async (dispatch: ViewsDispatch, getState: () => RootState) => {
    const oldQuery = selectQueryString(queryId)(getState());
    const multipleValues = hasMultipleValueForActions(contexts);
    const valuesToAdd: Array<ValueToAdd> = uniq(
      multipleValues
        ? contexts.valuePath.map((path) => {
            const [pathField, pathValue] = Object.entries(path)[0];

            return { field: pathField, value: pathValue, type: fieldTypeFor(pathField, contexts?.fieldTypes) };
          })
        : [{ field, value, type }],
    );

    let newQuery: string;

    if (multipleValues && contexts?.valuePathOperator === 'EDGE') {
      newQuery = addToQuery(
        oldQuery,
        edgeClause(
          { field: valuesToAdd[0].field, value: valuesToAdd[0].value },
          { field: valuesToAdd[1].field, value: valuesToAdd[1].value },
        ),
      );
    } else if (multipleValues && contexts?.valuePathOperator === 'OR') {
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
