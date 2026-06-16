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

import { escape, addToQuery, predicate, not, concatQueryStrings } from 'views/logic/queries/QueryHelper';
import recordQueryStringUsage from 'views/logic/queries/recordQueryStringUsage';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import type { RootState } from 'views/types';
import { updateQueryString } from 'views/logic/slices/viewSlice';
import { selectQueryString } from 'views/logic/slices/viewSelectors';
import hasMultipleValueForActions from 'views/components/visualizations/utils/hasMultipleValueForActions';
import type { ValuePath } from 'views/logic/valueactions/ValueActionHandler';
import type Widget from 'views/logic/widgets/Widget';

const formatNewQuery = (oldQuery: string, field: string, value: any) =>
  addToQuery(oldQuery, not(predicate(field, escape(value))));

// Negates a single OR group of all value path entries, e.g. `NOT (source:a OR target:a)` — exclude
// every message where the value appears in any of the configured groupings.
const excludeOrClause = (oldQuery: string, valuePath: ValuePath) => {
  const orGroup = concatQueryStrings(
    valuePath.map((path) => {
      const [pathField, pathValue] = Object.entries(path)[0];

      return predicate(pathField, escape(pathValue));
    }),
    { operator: 'OR', withBrackets: false },
  );

  return addToQuery(oldQuery, not(`(${orGroup})`));
};

type Args = {
  queryId: string;
  field: string;
  value?: string;
  contexts?: { valuePath?: ValuePath; valuePathOperator?: 'AND' | 'OR'; widget?: Widget } | null;
};

const ExcludeFromQueryHandler =
  ({ queryId, field, value, contexts }: Args) =>
  async (dispatch: ViewsDispatch, getState: () => RootState) => {
    const oldQuery = selectQueryString(queryId)(getState());
    const multipleValues = hasMultipleValueForActions(contexts);

    const valuesToAdd = uniq(multipleValues ? contexts.valuePath.map(() => ({ field, value })) : [{ field, value }]);

    const newQuery =
      multipleValues && contexts?.valuePathOperator === 'OR'
        ? excludeOrClause(oldQuery, contexts.valuePath)
        : valuesToAdd.reduce((prev, valueToAdd) => formatNewQuery(prev, valueToAdd.field, valueToAdd.value as string | number), oldQuery);

    await recordQueryStringUsage(newQuery, oldQuery);

    return dispatch(updateQueryString(queryId, newQuery));
  };

export default ExcludeFromQueryHandler;
