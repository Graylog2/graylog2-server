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
import React, { useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import type { Layout } from 'plotly.js';

import toPlotly from 'views/logic/aggregationbuilder/visualizations/Interpolation';
import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import useChartData from 'views/components/visualizations/useChartData';
import useEvents from 'views/components/visualizations/useEvents';
import { keySeparator, humanSeparator } from 'views/Constants';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import {
  generateDomain, generateLayouts,
  generateMappersForYAxis,
  getHoverTemplateSettings,
} from 'views/components/visualizations/utils/chartLayoytGenerators';
import useWidgetUnits from 'views/components/visualizations/hooks/useWidgetUnits';
import getSeriesUnit from 'views/components/visualizations/utils/getSeriesUnit';
import convertDataToBaseUnit from 'views/components/visualizations/utils/convertDataToBaseUnit';
import useFeature from 'hooks/useFeature';

import XYPlot from '../XYPlot';
import type { Generator } from '../ChartData';

const AreaVisualization = makeVisualization(({
  config,
  data,
  effectiveTimerange,
  height,
}: VisualizationComponentProps) => {
  const unitFeatureEnabled = useFeature('configuration_of_formatting_value');
  const widgetUnits = useWidgetUnits(config);
  const visualizationConfig = (config.visualizationConfig || AreaVisualizationConfig.empty()) as AreaVisualizationConfig;
  const { seriesUnitMapper, yAxisMapper, unitTypeMapper } = useMemo(() => generateMappersForYAxis({ series: config.series, units: widgetUnits }), [config.series, widgetUnits]);
  const { interpolation = 'linear' } = visualizationConfig;
  const mapKeys = useMapKeys();
  const rowPivotFields = useMemo(() => config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [], [config?.rowPivots]);
  const _mapKeys = useCallback((labels: string[]) => labels
    .map((label) => label.split(keySeparator)
      .map((l, i) => mapKeys(l, rowPivotFields[i]))
      .join(humanSeparator),
    ), [mapKeys, rowPivotFields]);

  const getExtendedChartGeneratorSettings = useCallback(({ originalName, name, values }: { originalName: string, name: string, values: Array<any> }) => {
    if (!unitFeatureEnabled) return ({});

    const yaxis = yAxisMapper[name];
    const curUnit = getSeriesUnit(config.series, name || originalName, widgetUnits);
    const convertedToBaseUnitValues = convertDataToBaseUnit(values, curUnit);

    return ({
      yaxis,
      y: convertedToBaseUnitValues,
      ...getHoverTemplateSettings({ curUnit, convertedToBaseValues: convertedToBaseUnitValues, originalName }),
    });
  }, [config.series, unitFeatureEnabled, widgetUnits, yAxisMapper]);

  const chartGenerator: Generator = useCallback(({ type, name, labels, values, originalName }) => ({
    type,
    name,
    x: _mapKeys(labels),
    y: values,
    fill: 'tozeroy',
    line: { shape: toPlotly(interpolation) },
    originalName,
    ...getExtendedChartGeneratorSettings({ originalName, name, values }),
  }), [_mapKeys, getExtendedChartGeneratorSettings, interpolation]);

  const rows = useMemo(() => retrieveChartData(data), [data]);

  const _chartDataResult = useChartData(rows, {
    widgetConfig: config,
    chartType: 'scatter',
    generator: chartGenerator,
  });

  const { eventChartData, shapes } = useEvents(config, data.events);

  const chartDataResult = useMemo(() => (eventChartData ? [..._chartDataResult, eventChartData] : _chartDataResult), [_chartDataResult, eventChartData]);
  const getLayoutExtendedSettings = useCallback(() => {
    if (!unitFeatureEnabled) return ({});

    const generatedLayouts = generateLayouts({ unitTypeMapper, seriesUnitMapper, chartData: chartDataResult });
    const _layouts: Partial<Layout> = ({
      ...generatedLayouts,
      hovermode: 'x',
      xaxis: { domain: generateDomain(Object.keys(unitTypeMapper)?.length) },
    });

    return _layouts;
  }, [chartDataResult, seriesUnitMapper, unitFeatureEnabled, unitTypeMapper]);
  const layout = useMemo<Partial<Layout>>(() => {
    const _layouts = shapes ? { shapes } : {};

    return ({ ..._layouts, ...getLayoutExtendedSettings() });
  }, [shapes, getLayoutExtendedSettings]);

  return (
    <XYPlot config={config}
            axisType={visualizationConfig.axisType}
            plotLayout={layout}
            effectiveTimerange={effectiveTimerange}
            height={height}
            chartData={chartDataResult} />
  );
}, 'area');

AreaVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: AggregationResult.isRequired,
  height: PropTypes.number,
};

export default AreaVisualization;
