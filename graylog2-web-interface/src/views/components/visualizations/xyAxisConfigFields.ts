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
import capitalize from 'lodash/capitalize';

import type {
  XYVisualizationConfigFormValues,
  XYVisualization,
} from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import type { ConfigurationField, FieldUnitType, CustomField } from 'views/types';
import { DEFAULT_AXIS_KEY } from 'views/components/visualizations/Constants';
import AxisVisualizationField from 'views/components/aggregationwizard/visualization/configurationFields/AxisVisualizationField';

export const getNumericAxisMetrics = (values: WidgetConfigFormValues) => {
  const units = values?.units ?? {};
  const metrics = values?.metrics ?? [];

  return metrics
    .filter(({ field, function: metricFn }) => !(units?.[field]?.unitType && units?.[field]?.abbrev) && metricFn)
    .map((metric) => metric.name || `${metric.function}(${metric.field ?? ''})`);
};

export const getAxisMetrics = (unitType: FieldUnitType, values: WidgetConfigFormValues) => {
  const units = values?.units ?? {};
  const metrics = values?.metrics ?? [];

  const unitTypeFields = new Set(
    Object.entries(units)
      .filter(([_, unit]) => unit.unitType === unitType && unit.abbrev)
      .map(([field]) => field),
  );

  return metrics
    .filter(({ field, function: metricFn }) => unitTypeFields.has(field) && metricFn)
    .map((metric) => metric.name || `${metric.function}(${metric.field ?? ''})`);
};

const getUnitTypeFields = (): Array<CustomField> =>
  ['percent', 'time', 'size'].map((unitType: FieldUnitType) => ({
    name: `axisConfig.${unitType}`,
    title: `Y-${capitalize(unitType)}`,
    id: unitType,
    type: 'custom',
    component: AxisVisualizationField,
    isShown: (formValues: XYVisualizationConfigFormValues, widgetConfigFormValues: WidgetConfigFormValues) =>
      formValues.showAxisLabels && !!getAxisMetrics(unitType, widgetConfigFormValues).length,
    inputHelp: (_: XYVisualizationConfigFormValues, widgetConfigFormValues: WidgetConfigFormValues) =>
      getAxisMetrics(unitType, widgetConfigFormValues).join(', '),
  }));

export const fromAxisConfig = (config: XYVisualization) => ({
  showAxisLabels: Object.values(config?.axisConfig ?? {}).some(({ title }) => !!title),
  axisConfig: config?.axisConfig,
});

const xyAxisConfigFields: Array<ConfigurationField> = [
  {
    name: 'showAxisLabels',
    title: 'Show axis labels',
    type: 'boolean',
  },
  {
    name: 'axisConfig.xaxis',
    title: 'X-axis',
    type: 'custom',
    id: 'xaxis',
    component: AxisVisualizationField,
    isShown: (formValues: XYVisualizationConfigFormValues) => formValues.showAxisLabels,
  },
  {
    name: `axisConfig.${DEFAULT_AXIS_KEY}`,
    title: 'Y-Number',
    type: 'custom',
    id: 'number',
    component: AxisVisualizationField,
    isShown: (formValues: XYVisualizationConfigFormValues, widgetConfigFormValues: WidgetConfigFormValues) =>
      !!getNumericAxisMetrics(widgetConfigFormValues).length && formValues.showAxisLabels,
    inputHelp: (_: XYVisualizationConfigFormValues, widgetConfigFormValues: WidgetConfigFormValues) =>
      getNumericAxisMetrics(widgetConfigFormValues).join(', '),
  },
  ...getUnitTypeFields(),
];

export default xyAxisConfigFields;
