import React from 'react';
import PropTypes from 'prop-types';
import { fromPairs, get, zip } from 'lodash';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import WorldMapVisualizationConfig from 'enterprise/logic/aggregationbuilder/visualizations/WorldMapVisualizationConfig';
import Viewport from 'enterprise/logic/aggregationbuilder/visualizations/Viewport';
import MapVisualization from './MapVisualization';
import { extractSeries } from '../Series';
import transformKeys from '../TransformKeys';

const arrayToMap = ([name, x, y]) => ({ name, x, y });
const lastKey = keys => keys[keys.length - 1];
const mergeObject = (prev, last) => Object.assign({}, prev, last);

const WorldMapVisualization = ({ config, data, onChange, width, ...rest }) => {
  const { rowPivots } = config;
  const series = extractSeries(transformKeys(config.rowPivots, config.columnPivots, data))
    .map(arrayToMap)
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
    onChange(visualizationConfig);
  };

  return (
    <MapVisualization {...rest}
                      data={series}
                      id={`worldmap-${config.id}`}
                      viewport={viewport}
                      width={width}
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
