// @flow strict
import React, { useCallback } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment-timezone';
import { get, merge } from 'lodash';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import Query from 'views/logic/queries/Query';

import GenericPlot from './GenericPlot';
import OnZoom from './OnZoom';
import CustomPropTypes from '../CustomPropTypes';
import type { ChartColor, ChartConfig, ColorMap } from './GenericPlot';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

type Props = {
  config: AggregationWidgetConfig,
  chartData: any,
  currentQuery: Query,
  timezone: string,
  effectiveTimerange: {
    from: string,
    to: string,
  },
  getChartColor?: (Array<ChartConfig>, string) => ?string,
  setChartColor?: (ChartConfig, ColorMap) => ChartColor,
  plotLayout?: any,
  onZoom: (Query, string, string) => boolean,
};

const XYPlot = ({
  config,
  chartData,
  currentQuery,
  timezone,
  effectiveTimerange,
  getChartColor,
  setChartColor,
  plotLayout = {},
  onZoom = OnZoom,
}: Props) => {
  const yaxis = { fixedrange: true, rangemode: 'tozero' };

  const layout = merge({}, { yaxis }, plotLayout);

  const _onZoom = useCallback(config.isTimeline ? (from, to) => onZoom(currentQuery, from, to) : () => true, [config.isTimeline, onZoom]);

  if (config.isTimeline) {
    const normalizedFrom = moment.tz(effectiveTimerange.from, timezone).format();
    const normalizedTo = moment.tz(effectiveTimerange.to, timezone).format();
    layout.xaxis = {
      range: [normalizedFrom, normalizedTo],
      type: 'date',
    };
  } else {
    layout.xaxis = {
      fixedrange: true,
      /* disable plotly sorting by setting the type of the xaxis to category */
      type: config.sort.length > 0 ? 'category' : undefined,
    };
  }

  return (
    <GenericPlot chartData={chartData}
                 layout={layout}
                 onZoom={_onZoom}
                 getChartColor={getChartColor}
                 setChartColor={setChartColor} />
  );
};

XYPlot.propTypes = {
  chartData: PropTypes.array.isRequired,
  config: CustomPropTypes.instanceOf(AggregationWidgetConfig).isRequired,
  timezone: PropTypes.string.isRequired,
  currentQuery: CustomPropTypes.instanceOf(Query).isRequired,
  effectiveTimerange: PropTypes.shape({
    from: PropTypes.string.isRequired,
    to: PropTypes.string.isRequired,
  }).isRequired,
  plotLayout: PropTypes.object,
  getChartColor: PropTypes.func.isRequired,
  setChartColor: PropTypes.func.isRequired,
  onZoom: PropTypes.func,
};

XYPlot.defaultProps = {
  plotLayout: {},
  onZoom: OnZoom,
};

export default connect(XYPlot, {
  currentQuery: CurrentQueryStore,
  currentUser: CurrentUserStore,
}, ({ currentQuery, currentUser }) => ({
  currentQuery,
  timezone: get(currentUser, ['currentUser', 'timezone'], 'UTC'),
}));
