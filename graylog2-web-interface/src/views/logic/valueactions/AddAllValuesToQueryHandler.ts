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
import { addToQuery } from 'views/logic/queries/QueryHelper';
import { updateQueryString } from 'views/logic/slices/viewSlice';
import { selectQueryString } from 'views/logic/slices/viewSelectors';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import type { RootState } from 'views/types';
import { fieldValueAndClause } from 'views/logic/valueactions/ValueActionQueryHelper';

type Arguments = {
  queryId: string;
  field: string;
  value?: Array<string | number>;
  type: FieldType;
};

// Adds all values of a multi-value field as `field:(a AND b AND c)`.
const AddAllValuesToQueryHandler =
  ({ queryId, field, value = [], type }: Arguments) =>
  async (dispatch: ViewsDispatch, getState: () => RootState) => {
    const oldQuery = selectQueryString(queryId)(getState());
    const values = uniq(value).map((v) => ({ value: v, type }));
    const newQuery = addToQuery(oldQuery, fieldValueAndClause(field, values));

    await recordQueryStringUsage(newQuery, oldQuery);

    return dispatch(updateQueryString(queryId, newQuery));
  };

export default AddAllValuesToQueryHandler;
