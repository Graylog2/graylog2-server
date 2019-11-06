// @flow strict
import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { flow, fromPairs, get, zip, isEmpty } from 'lodash';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import WorldMapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/WorldMapVisualizationConfig';
import Viewport from 'views/logic/aggregationbuilder/visualizations/Viewport';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';

import MapVisualization from './MapVisualization';
import { extractSeries, formatSeries, getLeafsFromRows, getXLabelsFromLeafs } from '../ChartData';
import transformKeys from '../TransformKeys';
import RenderCompletionCallback from '../../widgets/RenderCompletionCallback';

const _arrayToMap = ([name, x, y]) => ({ name, x, y });
const _lastKey = keys => keys[keys.length - 1];
const _mergeObject = (prev, last) => Object.assign({}, prev, last);
const _generateSeriesWithoutMetric = (rows: Rows) => {
  const leafs = getLeafsFromRows(rows);
  const xLabels = getXLabelsFromLeafs(leafs);
  return { valuesBySeries: { 'No metric defined': xLabels.map(() => 0) }, xLabels };
};

const WorldMapVisualization: VisualizationComponent = ({ config, data, editing, onChange, width, ...rest }: VisualizationComponentProps) => {
  let pipeline;
  const { rowPivots } = config;
  const onRenderComplete = useContext(RenderCompletionCallback);
  const hasMetric = !isEmpty(config._value.series);
  const markerRadiusSize = !hasMetric ? 1 : undefined;

  if (hasMetric) {
    pipeline = flow([
      transformKeys(config.rowPivots, config.columnPivots),
      extractSeries(),
      formatSeries,
      results => results.map(_arrayToMap),
    ]);
  } else {
    pipeline = flow([
      transformKeys(config.rowPivots, config.columnPivots),
      _generateSeriesWithoutMetric,
      formatSeries,
      results => results.map(_arrayToMap),
    ]);
  }
  const series = pipeline(data).map(({ name, x, y }) => {
    const newX = x.map(_lastKey);
    const keys = x.map(k => k.slice(0, -1)
      .map((key, idx) => ({ [rowPivots[idx].field]: key }))
      .reduce(_mergeObject, {}));
    return { name, y, x: newX, keys };
  })
    .map(({ keys, name, x, y }) => {
      // eslint-disable-next-line no-unused-vars
      const values = fromPairs(zip(x, y).filter(([_, v]) => (v !== undefined)));
      return { keys, name, values };
    });

  const viewport = get(config, 'visualizationConfig.viewport');
  const _onChange = (newViewport) => {
    const visualizationConfig = (config.visualizationConfig ? config.visualizationConfig.toBuilder() : WorldMapVisualizationConfig.builder())
      .viewport(Viewport.create(newViewport.center, newViewport.zoom))
      .build();
    if (editing) {
      onChange(visualizationConfig);
    }
  };

  return (
    <MapVisualization {...rest}
                      data={series}
                      id="world-map"
                      viewport={viewport}
                      width={width}
                      onRenderComplete={onRenderComplete}
                      markerRadiusSize={markerRadiusSize}
                      onChange={_onChange} />
  );
};

WorldMapVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.any.isRequired,
  onChange: PropTypes.func.isRequired,
  width: PropTypes.number.isRequired,
};

WorldMapVisualization.type = 'map';

export default WorldMapVisualization;
