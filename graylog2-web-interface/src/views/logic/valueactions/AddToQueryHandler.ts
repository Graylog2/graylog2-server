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
import { escape, addToQuery, formatTimestamp, predicate } from 'views/logic/queries/QueryHelper';
import { updateQueryString } from 'views/logic/slices/viewSlice';
import { selectQueryString } from 'views/logic/slices/viewSelectors';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import type { RootState, ActionContexts } from 'views/types';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';

const formatNewQuery = (oldQuery: string, field: string, value: string | number, type: FieldType) => {
  const predicateValue = type.type === 'date' ? formatTimestamp(value) : escape(value);

  return addToQuery(oldQuery, predicate(field, predicateValue));
};

type Arguments = {
  queryId: string;
  field: string;
  value?: string | number;
  type: FieldType;
  contexts?: ActionContexts;
};

const AddToQueryHandler =
  ({ queryId, field, value = '', type, contexts }: Arguments) =>
  async (dispatch: ViewsDispatch, getState: () => RootState) => {
    const oldQuery = selectQueryString(queryId)(getState());
    const valuesToAdd = uniq(contexts?.valuePath?.length ? contexts.valuePath : [{ [field]: value }]);

    const newQuery = valuesToAdd.reduce((prev, cur) => {
      const [curField, curValue] = Object.entries(cur)[0];
      const curType = fieldTypeFor(curField, contexts.fieldTypes) ?? type;

      return formatNewQuery(prev, curField, curValue as string | number, curType);
    }, oldQuery);

    return dispatch(updateQueryString(queryId, newQuery));
  };

export default AddToQueryHandler;
