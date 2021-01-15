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
import { WidgetActions } from 'views/stores/WidgetStore';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable/DataTable';
import type { CreatorProps } from 'views/components/sidebar/create/AddWidgetButton';
import { DEFAULT_TIMERANGE } from 'views/Constants';

export default function CreateCustomAggregation({ view }: CreatorProps) {
  const newWidget = AggregationWidget.builder()
    .newId()
    .timerange(view.type === View.Type.Dashboard ? DEFAULT_TIMERANGE : undefined)
    .config(AggregationWidgetConfig.builder()
      .rowPivots([])
      .series([])
      .visualization(DataTable.type)
      .build())
    .build();

  return WidgetActions.create(newWidget);
}
