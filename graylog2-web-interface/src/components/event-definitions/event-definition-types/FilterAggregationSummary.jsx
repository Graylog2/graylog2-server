import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import AggregationExpressionParser from 'logic/alerts/AggregationExpressionParser';

import { TIME_UNITS } from './FilterForm';

class FilterAggregationSummary extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
  };

  getConditionType = (config) => {
    const { group_by: groupBy, series, conditions } = config;
    return (lodash.isEmpty(groupBy)
    && (!conditions || lodash.isEmpty(conditions) || conditions.expression === null)
    && lodash.isEmpty(series)
      ? 'filter' : 'aggregation');
  };

  render() {
    const { config } = this.props;
    const {
      query,
      streams,
      search_within_ms: searchWithinMs,
      execute_every_ms: executeEveryMs,
      group_by: groupBy,
      series,
      conditions,
    } = config;

    const conditionType = this.getConditionType(config);

    const searchWithin = extractDurationAndUnit(searchWithinMs, TIME_UNITS);
    const executeEvery = extractDurationAndUnit(executeEveryMs, TIME_UNITS);

    const expressionResults = AggregationExpressionParser.parseExpression(conditions);

    return (
      <dl>
        <dt>Type</dt>
        <dd>{lodash.upperFirst(conditionType)}</dd>
        <dt>Search Query</dt>
        <dd>{query || '*'}</dd>
        <dt>Streams</dt>
        <dd>{streams && streams.length > 0 ? streams.join(', ') : 'No streams selected'}</dd>
        <dt>Search within</dt>
        <dd>{searchWithin.duration} {searchWithin.unit.toLowerCase()}</dd>
        <dt>Execute search every</dt>
        <dd>{executeEvery.duration} {executeEvery.unit.toLowerCase()}</dd>
        {conditionType === 'aggregation' && (
          <React.Fragment>
            <dt>Group by Field(s)</dt>
            <dd>{groupBy && groupBy.length > 0 ? groupBy.join(', ') : 'No Group by configured'}</dd>
            <dt>Create Events if</dt>
            <dd><em>{series[0].function}({series[0].field})</em> {expressionResults.operator} {expressionResults.value}</dd>
          </React.Fragment>
        )}
      </dl>
    );
  }
}

export default FilterAggregationSummary;
