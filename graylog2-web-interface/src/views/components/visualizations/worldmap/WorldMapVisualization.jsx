// @flow strict
import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { flow, fromPairs, get, zip, isEmpty } from 'lodash';

import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import WorldMapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/WorldMapVisualizationConfig';
import Viewport from 'views/logic/aggregationbuilder/visualizations/Viewport';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type Pivot from 'views/logic/aggregationbuilder/Pivot';
import { makeVisualization } from 'views/components/aggregationbuilder/AggregationBuilder';

import MapVisualization from './MapVisualization';

import { extractSeries, formatSeries, getLeafsFromRows, getXLabelsFromLeafs } from '../ChartData';
import transformKeys from '../TransformKeys';
import RenderCompletionCallback from '../../widgets/RenderCompletionCallback';

const _arrayToMap = ([name, x, y]) => ({ name, x, y });
const _lastKey = (keys) => keys[keys.length - 1];
const _mergeObject = (prev, last) => ({ ...prev, ...last });
const _createSeriesWithoutMetric = (rows: Rows) => {
  const leafs = getLeafsFromRows(rows);
  const xLabels = getXLabelsFromLeafs(leafs);

  if (!isEmpty(xLabels)) {
    return { valuesBySeries: { 'No metric defined': xLabels.map(() => null) }, xLabels };
  }

  return {};
};
const _formatSeriesForMap = (rowPivots: Array<Pivot>) => {
  return (result) => result.map(({ name, x, y }) => {
    const keys = x.map((k) => k.slice(0, -1)
      .map((key, idx) => ({ [rowPivots[idx].field]: key }))
      .reduce(_mergeObject, {}));
    const newX = x.map(_lastKey);
    // eslint-disable-next-line no-unused-vars
    const values = fromPairs(zip(newX, y).filter(([_, v]) => (v !== undefined)));

    return { keys, name, values };
  });
};

const WorldMapVisualization: VisualizationComponent = makeVisualization(({ config, data, editing, onChange, width, ...rest }: VisualizationComponentProps) => {
  const { rowPivots } = config;
  const onRenderComplete = useContext(RenderCompletionCallback);
  const hasMetric = !isEmpty(config.series);
  const markerRadiusSize = !hasMetric ? 1 : undefined;
  const seriesExtractor = hasMetric ? extractSeries() : _createSeriesWithoutMetric;

  const pipeline = flow([
    transformKeys(config.rowPivots, config.columnPivots),
    seriesExtractor,
    formatSeries,
    (results) => results.map(_arrayToMap),
    _formatSeriesForMap(rowPivots),
  ]);

  const rows = data.chart || Object.values(data)[0];

  const series = pipeline(rows);

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
}, 'map');

WorldMapVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: AggregationResult.isRequired,
  onChange: PropTypes.func.isRequired,
  width: PropTypes.number.isRequired,
};

export default WorldMapVisualization;
