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
import { isEmpty } from 'lodash';

import Series, { parseSeries } from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import { WidgetConfigFormValues, MetricFormValues } from './WidgetConfigForm';
import VisualizationConfiguration from './elementConfiguration/VisualizationConfiguration';
import GroupByConfiguration from './elementConfiguration/GroupByConfiguration';
import MetricsConfiguration from './elementConfiguration/MetricsConfiguration';
import SortConfiguration from './elementConfiguration/SortConfiguration';

export type AggregationElement = {
  title: string,
  key: string,
  isConfigured: (formValues: WidgetConfigFormValues) => boolean,
  multipleUse: boolean,
  order: number,
  toConfig?: (formValues: WidgetConfigFormValues, currentConfig: AggregationWidgetConfig) => AggregationWidgetConfig,
  fromConfig?: (config: AggregationWidgetConfig) => Partial<WidgetConfigFormValues>,
  onCreate?: () => void,
  onDeleteAll?: (formValues: WidgetConfigFormValues) => WidgetConfigFormValues,
  component: React.ComponentType<{
    config: AggregationWidgetConfig,
    onConfigChange: (newConfig: AggregationWidgetConfig) => void
  }>,
}

const _metricsToSeries = (formMetrics: Array<MetricFormValues>) => formMetrics
  .map((metric) => Series.create(metric.function, metric.field)
    .toBuilder()
    .config(SeriesConfig.empty().toBuilder().name(metric.name).build())
    .build());

const _seriesToMetrics = (series: Array<Series>) => series.map((s) => {
  const { type: func, field } = parseSeries(s.function);

  return {
    function: func,
    field,
    name: s.config?.name,
  };
});

const visualizationElement: AggregationElement = {
  title: 'Visualization',
  key: 'visualization',
  order: 4,
  multipleUse: false,
  isConfigured: (formValues: WidgetConfigFormValues) => !isEmpty(formValues.visualization),
  component: VisualizationConfiguration,
};

const metricElement: AggregationElement = {
  title: 'Metric',
  key: 'metrics',
  order: 2,
  multipleUse: true,
  isConfigured: (formValues: WidgetConfigFormValues) => !isEmpty(formValues.metrics),
  fromConfig: (providedConfig: AggregationWidgetConfig) => ({
    metrics: _seriesToMetrics(providedConfig.series),
  }),
  toConfig: (formValues: WidgetConfigFormValues, currentConfig: AggregationWidgetConfig) => currentConfig
    .toBuilder()
    .series(_metricsToSeries(formValues.metrics))
    .build(),
  component: MetricsConfiguration,
};

const groupByElement: AggregationElement = {
  title: 'Group By',
  key: 'groupBy',
  order: 1,
  multipleUse: true,
  isConfigured: (formValues: WidgetConfigFormValues) => !isEmpty(formValues.groupBy),
  component: GroupByConfiguration,
};

const sortElement: AggregationElement = {
  title: 'Sort',
  key: 'sort',
  order: 3,
  multipleUse: false,
  isConfigured: (formValues: WidgetConfigFormValues) => !isEmpty(formValues.sort),
  component: SortConfiguration,
};

export default [
  visualizationElement,
  metricElement,
  sortElement,
  groupByElement,
];
