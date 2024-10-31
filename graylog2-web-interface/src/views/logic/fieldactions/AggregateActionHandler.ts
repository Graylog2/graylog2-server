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
import Widget from 'views/logic/widgets/Widget';
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import DataTable from 'views/components/datatable';
import type { ThunkActionHandler } from 'views/components/actions/ActionHandler';
import type { AppDispatch } from 'stores/useAppDispatch';
import { addWidget } from 'views/logic/slices/widgetActions';

import duplicateCommonWidgetSettings from './DuplicateCommonWidgetSettings';

const AggregateActionHandler: ThunkActionHandler<{ widget?: Widget }> = ({
  field,
  type,
  contexts: { widget = Widget.empty() },
}) => (dispatch: AppDispatch) => {
  const newWidgetBuilder = AggregationWidget.builder()
    .newId()
    .config(AggregationWidgetConfig.builder()
      .rowPivots([pivotForField(field, type)])
      .series([Series.forFunction('count()'), Series.forFunction('percentage()')])
      .visualization(DataTable.type)
      .build());
  const newWidget = duplicateCommonWidgetSettings(newWidgetBuilder, widget).build();

  return dispatch(addWidget(newWidget));
};

export default AggregateActionHandler;
