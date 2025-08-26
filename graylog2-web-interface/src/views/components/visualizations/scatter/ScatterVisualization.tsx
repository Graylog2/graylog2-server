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
import * as React from 'react';
import { useCallback, useMemo } from 'react';
import type { Layout } from 'plotly.js';

import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import useChartData from 'views/components/visualizations/useChartData';
import useEvents from 'views/components/visualizations/useEvents';
import ScatterVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/ScatterVisualizationConfig';
import type { Generator } from 'views/components/visualizations/ChartData';
import useChartDataSettingsWithCustomUnits from 'views/components/visualizations/hooks/useChartDataSettingsWithCustomUnits';
import useChartLayoutSettingsWithCustomUnits from 'views/components/visualizations/hooks/useChartLayoutSettingsWithCustomUnits';
import usePlotOnClickPopover from 'views/components/visualizations/hooks/usePlotOnClickPopover';
import CartesianOnClickPopoverDropdown from 'views/components/visualizations/OnClickPopover/CartesianOnClickPopoverDropdown';
import OnClickPopoverWrapper from 'views/components/visualizations/OnClickPopover/OnClickPopoverWrapper';

import XYPlot from '../XYPlot';

const ScatterVisualization = makeVisualization(
  ({ config, data, effectiveTimerange, height, width }: VisualizationComponentProps) => {
    const visualizationConfig = (config.visualizationConfig ??
      ScatterVisualizationConfig.empty()) as ScatterVisualizationConfig;
    const getChartDataSettingsWithCustomUnits = useChartDataSettingsWithCustomUnits({ config });

    const rows = useMemo(() => retrieveChartData(data), [data]);
    const seriesGenerator: Generator = useCallback(
      ({ type, name, labels, values, originalName, fullPath }) => ({
        type,
        name,
        x: labels,
        y: values,
        mode: 'markers',
        originalName,
        ...getChartDataSettingsWithCustomUnits({ name, fullPath: fullPath, values }),
      }),
      [getChartDataSettingsWithCustomUnits],
    );
    const _chartDataResult = useChartData(rows, {
      widgetConfig: config,
      chartType: 'scatter',
      generator: seriesGenerator,
    });
    const { eventChartData, shapes } = useEvents(config, data.events);
    const chartDataResult = useMemo(
      () => (eventChartData ? [..._chartDataResult, eventChartData] : _chartDataResult),
      [_chartDataResult, eventChartData],
    );
    const getChartLayoutSettingsWithCustomUnits = useChartLayoutSettingsWithCustomUnits({
      config,
      chartData: chartDataResult,
    });
    const layout = useMemo<Partial<Layout>>(() => {
      const _layouts = shapes ? { shapes } : {};

      return { ..._layouts, ...getChartLayoutSettingsWithCustomUnits() };
    }, [shapes, getChartLayoutSettingsWithCustomUnits]);
    const { pos, onPopoverChange, isPopoverOpen, initializeGraphDivRef, onChartClick, clickPoint } =
      usePlotOnClickPopover('scatter');

    return (
      <>
        <XYPlot
          config={config}
          axisType={visualizationConfig.axisType}
          chartData={chartDataResult}
          plotLayout={layout}
          height={height}
          width={width}
          effectiveTimerange={effectiveTimerange}
          onClickMarker={onChartClick}
          onInitialized={initializeGraphDivRef}
        />
        <OnClickPopoverWrapper isPopoverOpen={isPopoverOpen} onPopoverChange={onPopoverChange} pos={pos}>
          <CartesianOnClickPopoverDropdown clickPoint={clickPoint} config={config} />
        </OnClickPopoverWrapper>
      </>
    );
  },
  'scatter',
);

export default ScatterVisualization;
