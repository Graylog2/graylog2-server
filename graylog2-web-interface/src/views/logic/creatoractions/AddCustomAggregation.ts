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
import View from 'views/logic/views/View';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable';
import type { CreatorProps } from 'views/components/sidebar/create/AddWidgetButton';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import type { AppDispatch } from 'stores/useAppDispatch';
import { addWidget } from 'views/logic/slices/widgetActions';
import type { GetState } from 'views/types';
import { selectView } from 'views/logic/slices/viewSelectors';

export const CreateCustomAggregation = ({ view }: CreatorProps) => AggregationWidget.builder()
  .newId()
  .timerange(view.type === View.Type.Dashboard ? DEFAULT_TIMERANGE : undefined)
  .config(AggregationWidgetConfig.builder()
    .rowPivots([])
    .series([])
    .visualization(DataTable.type)
    .build())
  .build();

export default () => (dispatch: AppDispatch, getState: GetState) => {
  const view = selectView(getState());

  return dispatch(addWidget(CreateCustomAggregation({ view })));
};
