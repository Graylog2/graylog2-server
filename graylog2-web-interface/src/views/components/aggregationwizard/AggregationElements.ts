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
  validate: (formValues: WidgetConfigFormValues) => { [key: string]: string },
}

const _metricsToSeries = (formMetrics: Array<MetricFormValues>) => formMetrics
  .map((metric) => Series.create(metric.function, metric.field, metric.percentile)
    .toBuilder()
    .config(SeriesConfig.empty().toBuilder().name(metric.name).build())
    .build());

const _seriesToMetrics = (series: Array<Series>) => series.map((s) => {
  const { type: func, field, percentile } = parseSeries(s.function);

  const metric = {
    function: func,
    field,
    name: s.config?.name,
  };

  if (percentile) {
    const parsedPercentile = Number.parseFloat(percentile);

    return {
      ...metric,
      percentile: parsedPercentile,
    };
  }

  return metric;
});

const visualizationElement: AggregationElement = {
  title: 'Visualization',
  key: 'visualization',
  order: 4,
  multipleUse: false,
  isConfigured: (formValues: WidgetConfigFormValues) => !isEmpty(formValues.visualization),
  component: VisualizationConfiguration,
};

type MetricError = {
  function?: string,
  field?: string,
  percentile?: string,
};

const hasErrors = <T extends {}>(errors: Array<T>): boolean => {
  return errors.filter((error) => Object.keys(error).length > 0).length > 0;
};

const validateMetrics = (values: WidgetConfigFormValues) => {
  const errors = {};

  if (!values.metrics) {
    return errors;
  }

  const metricsErrors = values.metrics.map((metric) => {
    const metricError: MetricError = {};

    if (!metric.function) {
      metricError.function = 'Function is required.';
    }

    const isFieldRequired = metric.function && metric.function !== 'count';

    if (isFieldRequired && !metric.field) {
      metricError.field = `Field is required for function ${metric.function}.`;
    }

    if (metric.function === 'percentile' && !metric.percentile) {
      metricError.percentile = 'Percentile is required.';
    }

    return metricError;
  });

  return hasErrors(metricsErrors) ? { metrics: metricsErrors } : {};
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
  validate: validateMetrics,
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
