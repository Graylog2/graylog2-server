// @flow strict
import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { flow, fromPairs, get, zip } from 'lodash';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import WorldMapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/WorldMapVisualizationConfig';
import Viewport from 'views/logic/aggregationbuilder/visualizations/Viewport';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';

import MapVisualization from './MapVisualization';
import { extractSeries } from '../ChartData';
import transformKeys from '../TransformKeys';
import RenderCompletionCallback from '../../widgets/RenderCompletionCallback';

const arrayToMap = ([name, x, y]) => ({ name, x, y });
const lastKey = keys => keys[keys.length - 1];
const mergeObject = (prev, last) => Object.assign({}, prev, last);

const WorldMapVisualization: VisualizationComponent = ({ config, data, editing, onChange, width, ...rest }: VisualizationComponentProps) => {
  const onRenderComplete = useContext(RenderCompletionCallback);
  const { rowPivots } = config;
  const pipeline = flow([
    transformKeys(config.rowPivots, config.columnPivots),
    extractSeries(),
    results => results.map(arrayToMap),
  ]);
  const series = pipeline(data)
    .map(({ name, x, y }) => {
      const newX = x.map(lastKey);
      const keys = x.map(k => k.slice(0, -1)
        .map((key, idx) => ({ [rowPivots[idx].field]: key }))
        .reduce(mergeObject, {}));
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
