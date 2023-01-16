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

import type Query from 'views/logic/queries/Query';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import type { ViewType } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import { adjustFormat, toUTCFromTz } from 'util/DateTime';
import type { AppDispatch } from 'stores/useAppDispatch';
import { setGlobalOverrideTimerange, execute } from 'views/logic/slices/searchExecutionSlice';
import { setTimerange } from 'views/logic/slices/viewSlice';

const onZoom = (dispatch: AppDispatch, currentQuery: Query, from: string, to: string, viewType: ViewType | undefined | null, userTz: string) => {
  const newTimeRange: AbsoluteTimeRange = {
    type: 'absolute',
    from: adjustFormat(toUTCFromTz(from, userTz), 'internal'),
    to: adjustFormat(toUTCFromTz(to, userTz), 'internal'),
  };

  if (viewType === View.Type.Dashboard) {
    dispatch(setGlobalOverrideTimerange(newTimeRange)).then(() => dispatch(execute()));
  } else {
    dispatch(setTimerange(currentQuery.id, newTimeRange));
  }

  return false;
};

export default onZoom;
