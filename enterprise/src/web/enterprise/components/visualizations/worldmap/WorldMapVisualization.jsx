import React from 'react';
import PropTypes from 'prop-types';
import { fromPairs, zip } from 'lodash';

import MapVisualization from './MapVisualization';
import { extractSeries } from '../Series';
import { transformKeys } from '../TransformKeys';

const WorldMapVisualization = ({ config, data, width, ...rest }) => {
  const { rowPivots } = config;
  const series = extractSeries(transformKeys(config.rowPivots, config.columnPivots, data))
    .map(([name, x, y]) => ({ name, x, y }))
    .map(({ name, x, y }) => {
      const newX = x.map(keys => keys[keys.length - 1]);
      const keys = x.map(k => k.slice(0, -1)
        .map((key, idx) => ({ [rowPivots[idx].field]: key }))
        .reduce((prev, last) => Object.assign({}, prev, last), {}),
      );
      return { name, y, x: newX, keys };
    })
    .map(({ keys, name, x, y }) => {
      const values = fromPairs(zip(x, y).filter(([_, v]) => (v !== undefined)));
      return { keys, name, values };
    });

  return <MapVisualization {...rest} data={series} id={`worldmap-${config.id}`} />;
};

WorldMapVisualization.propTypes = {};

WorldMapVisualization.type = 'map';

export default WorldMapVisualization;
