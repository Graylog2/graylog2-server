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
import React, { useContext } from 'react';
import flow from 'lodash/flow';
import fromPairs from 'lodash/fromPairs';
import get from 'lodash/get';
import zip from 'lodash/zip';
import isEmpty from 'lodash/isEmpty';

import type Viewport from 'views/logic/aggregationbuilder/visualizations/Viewport';
import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type Pivot from 'views/logic/aggregationbuilder/Pivot';
import useUserDateTime from 'hooks/useUserDateTime';

import MapVisualization from './MapVisualization';

import type { ExtractedSeries, ChartData } from '../ChartData';
import { extractSeries, formatSeries, getLeafsFromRows, getXLabelsFromLeafs } from '../ChartData';
import transformKeys from '../TransformKeys';
import RenderCompletionCallback from '../../widgets/RenderCompletionCallback';

const _arrayToMap = ([name, x, y]: ChartData) => ({ name, x, y });
const _lastKey = <T, >(keys: Array<T>) => keys[keys.length - 1];

const _createSeriesWithoutMetric = (rows: Rows) => {
  const leafs = getLeafsFromRows(rows);
  const xLabels = getXLabelsFromLeafs(leafs);

  if (!isEmpty(xLabels)) {
    return { valuesBySeries: { 'No metric defined': xLabels.map(() => null) }, xLabels };
  }

  return {};
};

const _formatSeriesForMap = (rowPivots: Array<Pivot>) => {
  const fields = rowPivots.flatMap((rowPivot) => rowPivot.fields);

  return (result: Array<ReturnType<typeof _arrayToMap>>) => result.map(({ name, x, y }) => {
    const keys = x.map((k) => Object.fromEntries(k.slice(0, -1)
      .map((key, idx) => [fields[idx], key])));
    const newX = x.map(_lastKey);

    const values = fromPairs(zip(newX, y).filter(([_, v]) => (v !== undefined)));

    return { keys, name, values };
  });
};

const WorldMapVisualization = makeVisualization(({
  config,
  data,
  editing,
  onChange,
  width,
  ...rest
}: VisualizationComponentProps) => {
  const { rowPivots } = config;
  const onRenderComplete = useContext(RenderCompletionCallback);
  const hasMetric = !isEmpty(config.series);
  const markerRadiusSize = !hasMetric ? 1 : undefined;
  const seriesExtractor = hasMetric ? extractSeries() : _createSeriesWithoutMetric;
  const { formatTime } = useUserDateTime();

  const pipeline = flow([
    transformKeys(config.rowPivots, config.columnPivots, formatTime),
    seriesExtractor,
    formatSeries,
    (results: ExtractedSeries) => results.map(_arrayToMap),
    _formatSeriesForMap(rowPivots),
  ]);

  const rows = retrieveChartData(data);

  const series = pipeline(rows);

  const viewport = get(config, 'visualizationConfig.viewport');

  const _onChange = (newViewport: Viewport) => {
    if (editing) {
      onChange({
        zoom: newViewport.zoom,
        centerX: newViewport.center[0],
        centerY: newViewport.center[1],
      });
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

export default WorldMapVisualization;
