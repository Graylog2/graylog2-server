// @flow strict
import React, { useCallback } from 'react';
import PropTypes from 'prop-types';

import EventHandler from 'views/logic/searchtypes/events/EventHandler';
import toPlotly from 'views/logic/aggregationbuilder/visualizations/Interpolation';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import type { ChartDefinition } from '../ChartData';
import XYPlot from '../XYPlot';
import { chartData } from '../ChartData';

const getChartColor = (fullData, name) => {
  const data = fullData.find(d => (d.name === name));
  if (data && data.line && data.line.color) {
    const { line: { color } } = data;
    return color;
  }
  return undefined;
};

const setChartColor = (chart, colors) => ({ line: { color: colors[chart.name] } });

const AreaVisualization: VisualizationComponent = ({ config, data, effectiveTimerange }: VisualizationComponentProps) => {
  // $FlowFixMe: We need to assume it is a LineVisualizationConfig instance
  const visualizationConfig: AreaVisualizationConfig = config.visualizationConfig || AreaVisualizationConfig.empty();
  const { interpolation = 'linear' } = visualizationConfig;
  const chartGenerator = useCallback((type, name, labels, values): ChartDefinition => ({
    type,
    name,
    x: labels,
    y: values,
    fill: 'tozeroy',
    line: { shape: toPlotly(interpolation) },
  }), [interpolation]);

  const chartDataResult = chartData(config, data.chart || Object.values(data)[0], 'scatter', chartGenerator);
  const layout = {};
  if (config.eventAnnotation && data.events) {
    const { eventChartData, shapes } = EventHandler.toVisualizationData(data.events, config.formattingSettings);
    chartDataResult.push(eventChartData);
    layout.shapes = shapes;
  }

  return (
    <XYPlot config={config}
            plotLayout={layout}
            effectiveTimerange={effectiveTimerange}
            getChartColor={getChartColor}
            setChartColor={setChartColor}
            chartData={chartDataResult} />
  );
};

AreaVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.objectOf(PropTypes.arrayOf(PropTypes.object)).isRequired,
};

AreaVisualization.type = 'area';

export default AreaVisualization;
