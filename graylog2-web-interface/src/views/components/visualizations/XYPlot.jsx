// @flow strict
import React, { useCallback, useContext } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment-timezone';
import { get, merge } from 'lodash';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import Query from 'views/logic/queries/Query';
import type { ViewType } from 'views/logic/views/View';

import GenericPlot from './GenericPlot';
import OnZoom from './OnZoom';
import CustomPropTypes from '../CustomPropTypes';
import type { ChartColor, ChartConfig, ColorMap } from './GenericPlot';
import ViewTypeContext from '../contexts/ViewTypeContext';

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
  height?: number;
  setChartColor?: (ChartConfig, ColorMap) => ChartColor,
  plotLayout?: any,
  onZoom: (Query, string, string, ?ViewType) => boolean,
};

const yLegendPosition = (containerHeight: number) => {
  if (containerHeight < 150) {
    return -0.6;
  }
  if (containerHeight < 400) {
    return -0.2;
  }
  return -0.14;
};

const XYPlot = ({
  config,
  chartData,
  currentQuery,
  timezone,
  effectiveTimerange,
  getChartColor,
  setChartColor,
  height,
  plotLayout = {},
  onZoom = OnZoom,
}: Props) => {
  const yaxis = { fixedrange: true, rangemode: 'tozero' };
  const defaultLayout: {yaxis: Object, legend?: Object} = { yaxis };
  if (height) {
    defaultLayout.legend = { y: yLegendPosition(height) };
  }
  const layout = merge({}, defaultLayout, plotLayout);
  const viewType = useContext(ViewTypeContext);
  const _onZoom = useCallback(config.isTimeline
    ? (from, to) => onZoom(currentQuery, from, to, viewType)
    : () => true, [config.isTimeline, onZoom]);

  if (config.isTimeline && effectiveTimerange) {
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
  }),
  plotLayout: PropTypes.object,
  getChartColor: PropTypes.func,
  setChartColor: PropTypes.func,
  onZoom: PropTypes.func,
};

XYPlot.defaultProps = {
  plotLayout: {},
  getChartColor: undefined,
  height: undefined,
  setChartColor: undefined,
  effectiveTimerange: undefined,
  onZoom: OnZoom,
};

export default connect(XYPlot, {
  currentQuery: CurrentQueryStore,
  currentUser: CurrentUserStore,
}, ({ currentQuery, currentUser }) => ({
  currentQuery,
  timezone: get(currentUser, ['currentUser', 'timezone'], 'UTC'),
}));
