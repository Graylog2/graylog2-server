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
import Immutable from 'immutable';

import ViewState from 'views/logic/views/ViewState';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';

// local storage mock for pinned-field-charts
const mockFieldCharts = ({
  renderer = 'line',
  interpolation = 'linear',
  interval = 'minute',
  valuetype = 'count',
  rangetype = 'relative',
  field = 'level',
}) => ({
  'field-chart-id': {
    renderer,
    interpolation,
    interval,
    valuetype,
    rangetype,
    field,
    query: '',
    chartid: 'field-chart-id',
    createdAt: 1575465944784,
    range: { relative: 300 },
  },
});

const viewState = () => {
  const widget1 = AggregationWidget.builder()
    .id('widget1')
    .config(AggregationWidgetConfig.builder().build())
    .build();
  const widget2 = AggregationWidget.builder()
    .id('widget2')
    .config(AggregationWidgetConfig.builder().build())
    .build();
  const widgets = Immutable.List([widget1, widget2]);
  const positions = {
    widget1: new WidgetPosition(1, 1, 2, Infinity),
    widget2: new WidgetPosition(1, 3, 6, Infinity),
  };

  return ViewState.create()
    .toBuilder()
    .widgets(widgets)
    .widgetPositions(positions)
    .build();
};

// eslint-disable-next-line import/prefer-default-export
export { mockFieldCharts, viewState };
