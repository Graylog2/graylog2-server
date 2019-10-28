// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import { set, flatten, uniqWith, isEqual } from 'lodash';
import { defaultCompare } from 'views/logic/DefaultCompare';
import type { Rows, Leaf } from 'views/logic/searchtypes/pivot/PivotHandler';
import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { ChartDefinition, ExtractedSeries, KeyJoiner } from '../ChartData';
import GenericPlot from '../GenericPlot';
import { chartData, flattenLeafs } from '../ChartData';


const _seriesGenerator = (type, name, labels, values, z): ChartDefinition => ({ type, name, x: labels, y: values, z });

const _compareArray = (ary1, ary2) => {
  if (ary1 === undefined) {
    if (ary2 === undefined) {
      return 0;
    }
    return -1;
  }
  if (ary1.length > ary2.length) {
    return 1;
  }
  if (ary1.length < ary2.length) {
    return -1;
  }
  const diffIdx = ary1.findIndex((v, idx) => (defaultCompare(v, ary2[idx]) !== 0));
  if (diffIdx === -1) {
    return 0;
  }
  return defaultCompare(ary1[diffIdx], ary2[diffIdx]);
};

const _extractColumnPivotValues = (rows): Array<Array<string>> => {
  const uniqRows = uniqWith(
    flatten(
      rows
        .filter(({ source }) => (source === 'leaf' || source === 'non-leaf'))
        // $FlowFixMe: Actually filtering out rows with single values
        .map(({ values }) => values),
    )
      // $FlowFixMe: Should be safe, even if rollup is not present
      .filter(({ rollup }) => !rollup)
      .map(({ key }) => key.slice(0, -1)),
    isEqual,
  );
  return Immutable.List(uniqRows).sort(_compareArray).toArray();
};

export const _seriesExtraction = (keyJoiner: KeyJoiner) => {
  return (results: Rows): ExtractedSeries => {
    // $FlowFixMe: Somehow flow is unable to infer that the result consists only of Leafs.
    const leafs: Array<Leaf> = results.filter(row => (row.source === 'leaf'));
    const xLabels = _extractColumnPivotValues(results);
    const yLabels = flatten(leafs.map(({ key }) => key));
    const flatLeafs = flattenLeafs(leafs, false);
    const valuesBySeries = {};
    flatLeafs.forEach(([key, value]) => {
      const joinedKey = keyJoiner(value.key);
      const targetIdx = xLabels.findIndex(l => isEqual(l, [joinedKey]));
      if (value.value) {
        set(valuesBySeries, [key[0], targetIdx], value.value);
      }
    });
    const z = Object.values(valuesBySeries);
    return [[
      'XYZ Chart',
      xLabels,
      yLabels,
      z,
    ]];
  };
};

const HeatmapVisualization: VisualizationComponent = ({ config, data }: VisualizationComponentProps) => {
  return (
    <GenericPlot chartData={chartData(config, data, 'heatmap', _seriesGenerator, _seriesExtraction)} />
  );
};

HeatmapVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

HeatmapVisualization.type = 'heatmap';

export default HeatmapVisualization;
