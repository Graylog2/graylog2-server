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
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import Series, { isFunction } from 'views/logic/aggregationbuilder/Series';
import { TIMESTAMP_FIELD } from 'views/Constants';
import type { ThunkActionHandler } from 'views/components/actions/ActionHandler';
import type { AppDispatch } from 'stores/useAppDispatch';
import { addWidget } from 'views/logic/slices/widgetActions';

import duplicateCommonWidgetSettings from './DuplicateCommonWidgetSettings';

import { FieldTypes } from '../fieldtypes/FieldType';

const ChartActionHandler: ThunkActionHandler<{ widget?: Widget }> = ({
  field,
  contexts: { widget: origWidget = Widget.empty() },
}) => (dispatch: AppDispatch) => {
  const series = isFunction(field) ? Series.forFunction(field) : Series.forFunction(`avg(${field})`);
  const config = AggregationWidgetConfig.builder()
    .rowPivots([pivotForField(TIMESTAMP_FIELD, FieldTypes.DATE())])
    .series([series])
    .visualization('line')
    .rollup(true)
    .build();
  const widgetBuilder = AggregationWidget.builder()
    .newId()
    .config(config);

  const widget = duplicateCommonWidgetSettings(widgetBuilder, origWidget).build();

  return dispatch(addWidget(widget));
};

export default ChartActionHandler;
