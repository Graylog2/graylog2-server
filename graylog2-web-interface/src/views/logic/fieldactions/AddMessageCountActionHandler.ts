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
import NumberVisualization from 'views/components/visualizations/number/NumberVisualization';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import type { ActionHandler } from 'views/components/actions/ActionHandler';

const AddMessageCountActionHandler: ActionHandler = () => {
  const series = Series.forFunction('count()')
    .toBuilder()
    .config(new SeriesConfig('Message Count'))
    .build();

  WidgetActions.create(AggregationWidget.builder()
    .newId()
    .config(AggregationWidgetConfig.builder()
      .series([series])
      .visualization(NumberVisualization.type)
      .build())
    .build());
};

export default AddMessageCountActionHandler;
