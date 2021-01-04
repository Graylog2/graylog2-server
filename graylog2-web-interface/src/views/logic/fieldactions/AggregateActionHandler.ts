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
import { WidgetActions } from 'views/stores/WidgetStore';
import Widget from 'views/logic/widgets/Widget';
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import DataTable from 'views/components/datatable/DataTable';

import type { FieldActionHandler } from './FieldActionHandler';
import duplicateCommonWidgetSettings from './DuplicateCommonWidgetSettings';

const AggregateActionHandler: FieldActionHandler = ({ field, type, contexts: { widget = Widget.empty() } }) => {
  const newWidgetBuilder = AggregationWidget.builder()
    .newId()
    .config(AggregationWidgetConfig.builder()
      .rowPivots([pivotForField(field, type)])
      .series([Series.forFunction('count()')])
      .visualization(DataTable.type)
      .build());
  const newWidget = duplicateCommonWidgetSettings(newWidgetBuilder, widget).build();

  return WidgetActions.create(newWidget);
};

export default AggregateActionHandler;
