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
import type { AggregationWidgetConfigBuilder } from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series, { parseSeries } from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';

import MetricsConfiguration from './MetricsConfiguration';

import type { AggregationElement } from '../AggregationElementType';
import type { WidgetConfigFormValues, MetricFormValues } from '../WidgetConfigForm';

type MetricError = {
  function?: string,
  field?: string,
  percentile?: string,
  name?: string,
};

const hasErrors = <T extends {}> (errors: Array<T>): boolean => errors.filter((error) => Object.keys(error).length > 0).length > 0;

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

    const isFieldRequired = metric.function !== 'count' && (metric.function !== 'percentage' || metric.strategy === 'SUM');

    if (isFieldRequired && !metric.field) {
      metricError.field = `Field is required for function ${metric.function}.`;
    }

    if (metric.function === 'percentile' && !metric.percentile) {
      metricError.percentile = 'Percentile is required.';
    }

    if (metric.name && values.metrics.filter(({ name }) => name === metric.name).length > 1) {
      metricError.name = 'Name must be unique.';
    }

    return metricError;
  });

  return hasErrors(metricsErrors) ? { metrics: metricsErrors } : {};
};

const parameterForMetric = (metric: MetricFormValues) => {
  switch (metric.function) {
    case 'percentage': return metric.strategy;
    case 'percentile': return metric.percentile;
    default: return undefined;
  }
};

const emptyToUndefined = (s: string) => (s?.trim() === '' ? undefined : s);

const metricsToSeries = (formMetrics: Array<MetricFormValues>) => formMetrics
  .map((metric) => Series.create(metric.function, emptyToUndefined(metric.field), parameterForMetric(metric))
    .toBuilder()
    .config(SeriesConfig.empty().toBuilder().name(metric.name).build())
    .build());

export const seriesToMetrics = (series: Array<Series>) => series.map((s: Series) => {
  const { type: func, field, percentile, strategy } = parseSeries(s.function) ?? {};

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

  if (strategy) {
    return {
      ...metric,
      strategy,
    };
  }

  return metric;
});

const MetricElement: AggregationElement<'metrics'> = {
  sectionTitle: 'Metrics',
  title: 'Metric',
  key: 'metrics',
  order: 2,
  allowCreate: () => true,
  fromConfig: (config: AggregationWidgetConfig) => ({
    metrics: seriesToMetrics(config.series),
  }),
  toConfig: (formValues: WidgetConfigFormValues, configBuilder: AggregationWidgetConfigBuilder) => configBuilder
    .series(metricsToSeries(formValues.metrics)),
  onRemove: ((index, formValues) => ({
    ...formValues,
    metrics: formValues.metrics.filter((_value, i) => index !== i),
  })),
  component: MetricsConfiguration,
  validate: validateMetrics,
  isEmpty: (formValues: WidgetConfigFormValues['metrics']) => (formValues ?? []).length === 0,
};

export default MetricElement;
