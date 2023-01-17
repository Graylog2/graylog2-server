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
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Widget from 'views/logic/widgets/Widget';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import Series from 'views/logic/aggregationbuilder/Series';
import TitleTypes from 'views/stores/TitleTypes';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { GetState } from 'views/types';
import { addWidget } from 'views/logic/slices/widgetActions';
import { setTitle } from 'views/logic/slices/titlesActions';
import { selectActiveQuery } from 'views/logic/slices/viewSelectors';
import type { ActionHandlerArguments } from 'views/components/actions/ActionHandler';

import duplicateCommonWidgetSettings from './DuplicateCommonWidgetSettings';

const NUMERIC_FIELD_SERIES = ['count', 'sum', 'avg', 'min', 'max', 'stddev', 'variance', 'card', 'percentile'];
const NONNUMERIC_FIELD_SERIES = ['count', 'card'];

const handler = ({
  field,
  type,
  contexts: { widget: origWidget = Widget.empty() },
}: ActionHandlerArguments<{ widget?: Widget }>) => (dispatch: AppDispatch, getState: GetState) => {
  const activeQuery = selectActiveQuery(getState());
  const series = ((type && type.isNumeric()) ? NUMERIC_FIELD_SERIES : NONNUMERIC_FIELD_SERIES)
    .map((f) => {
      if (f === 'percentile') {
        return `${f}(${field},95)`;
      }

      return `${f}(${field})`;
    })
    .map(Series.forFunction);
  const config = AggregationWidgetConfig.builder()
    .series(series)
    .visualization('table')
    .rollup(true)
    .build();
  const widgetBuilder = AggregationWidget.builder()
    .newId()
    .config(config);

  const widget = duplicateCommonWidgetSettings(widgetBuilder, origWidget).build();

  return dispatch(addWidget(widget))
    .then(() => dispatch(setTitle(activeQuery, TitleTypes.Widget, widget.id, `Field Statistics for ${field}`)));
};

export default handler;
