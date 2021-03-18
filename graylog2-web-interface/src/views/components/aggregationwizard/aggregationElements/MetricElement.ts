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

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series, { parseSeries } from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';

import type { AggregationElement } from './AggregationElementType';

import MetricsConfiguration from '../elementConfiguration/MetricsConfiguration';
import { WidgetConfigFormValues, MetricFormValues } from '../WidgetConfigForm';

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

const metricsToSeries = (formMetrics: Array<MetricFormValues>) => formMetrics
  .map((metric) => Series.create(metric.function, metric.field, metric.percentile)
    .toBuilder()
    .config(SeriesConfig.empty().toBuilder().name(metric.name).build())
    .build());

const seriesToMetrics = (series: Array<Series>) => series.map((s: Series) => {
  const { type: func, field, percentile } = parseSeries(s.function) ?? {};

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

const MetricElement: AggregationElement = {
  title: 'Metric',
  key: 'metrics',
  order: 2,
  allowCreate: () => true,
  fromConfig: (providedConfig: AggregationWidgetConfig) => ({
    metrics: seriesToMetrics(providedConfig.series),
  }),
  toConfig: (formValues: WidgetConfigFormValues, currentConfig: AggregationWidgetConfig) => currentConfig
    .toBuilder()
    .series(metricsToSeries(formValues.metrics))
    .build(),
  component: MetricsConfiguration,
  validate: validateMetrics,
};

export default MetricElement;
