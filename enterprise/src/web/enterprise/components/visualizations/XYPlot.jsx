import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment-timezone';
import { get } from 'lodash';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import { SearchActions, SearchStore } from 'enterprise/stores/SearchStore';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import { CurrentQueryStore } from 'enterprise/stores/CurrentQueryStore';
import { QueriesActions } from 'enterprise/stores/QueriesStore';
import Query from 'enterprise/logic/queries/Query';

import GenericPlot from './GenericPlot';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const onZoom = (config, currentQuery, currentUser, from, to) => {
  const { timezone } = currentUser;

  const newTimerange = {
    type: 'absolute',
    from: moment.tz(from, timezone).toISOString(),
    to: moment.tz(to, timezone).toISOString(),
  };

  QueriesActions.timerange(currentQuery.id, newTimerange).then(SearchActions.executeWithCurrentState);
  return false;
};

const XYPlot = ({ config, chartData, currentQuery, currentUser, effectiveTimerange }) => {
  const layout = {
    yaxis: {
      fixedrange: true,
    },
  };
  let _onZoom = () => {};
  if (config.isTimeline) {
    const { timezone } = currentUser;
    const normalizedFrom = moment.tz(effectiveTimerange.from, timezone).format();
    const normalizedTo = moment.tz(effectiveTimerange.to, timezone).format();
    layout.xaxis = {
      range: [normalizedFrom, normalizedTo],
    };
    _onZoom = (from, to) => onZoom(config, currentQuery, currentUser, from, to);
  } else {
    layout.xaxis = {
      fixedrange: true,
    };
  }
  return (
    <GenericPlot chartData={chartData} layout={layout} onZoom={_onZoom} />
  );
};

XYPlot.propTypes = {
  chartData: PropTypes.array.isRequired,
  config: PropTypes.instanceOf(AggregationWidgetConfig).isRequired,
  currentUser: PropTypes.shape({
    timezone: PropTypes.string.isRequired,
  }).isRequired,
  currentQuery: PropTypes.instanceOf(Query).isRequired,
  effectiveTimerange: PropTypes.shape({
    from: PropTypes.string.isRequired,
    to: PropTypes.string.isRequired,
  }).isRequired,
};

export default connect(XYPlot, {
  currentQuery: CurrentQueryStore,
  currentUser: CurrentUserStore,
  searches: SearchStore,
}, ({ currentQuery, currentUser, searches }) => ({
  currentQuery,
  currentUser: currentUser.currentUser,
  effectiveTimerange: get(searches.result.forId(currentQuery.id), ['effectiveTimerange'], {}),
}));
