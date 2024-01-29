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
import React from 'react';
import PropTypes from 'prop-types';
import isEmpty from 'lodash/isEmpty';
import upperFirst from 'lodash/upperFirst';

import { Link } from 'components/common/router';
import { Alert } from 'components/bootstrap';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import { isPermitted } from 'util/PermissionsMixin';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import Routes from 'routing/Routes';
import validateExpression from 'logic/alerts/AggregationExpressionValidation';

import AggregationConditionSummary from './AggregationConditionSummary';
import withStreams from './withStreams';
import { TIME_UNITS } from './FilterForm';
import styles from './FilterAggregationSummary.css';

import LinkToReplaySearch from '../replay-search/LinkToReplaySearch';

const formatStreamOrId = (streamOrId) => {
  if (typeof streamOrId === 'string') {
    return <span key={streamOrId}><em>{streamOrId}</em></span>;
  }

  return (
    <span key={streamOrId.id}>
      <Link to={Routes.stream_search(streamOrId.id)}>{streamOrId.title}</Link>
    </span>
  );
};

const getConditionType = (config) => {
  const { group_by: groupBy, series, conditions } = config;

  return (isEmpty(groupBy)
  && (!conditions || isEmpty(conditions) || conditions.expression === null)
  && isEmpty(series)
    ? 'filter' : 'aggregation');
};

const renderQueryParameters = (queryParameters) => {
  if (queryParameters.some((p) => p.embryonic)) {
    const undeclaredParameters = queryParameters.filter((p) => p.embryonic)
      .map((p) => p.name)
      .join(', ');

    return (
      <Alert bsStyle="danger">
        There are undeclared query parameters: {undeclaredParameters}
      </Alert>
    );
  }

  return <dd>{queryParameters.map((p) => p.name).join(', ')}</dd>;
};

class FilterAggregationSummary extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    streams: PropTypes.array.isRequired,
  };

  renderStreams = (streamIds, streamIdsWithMissingPermission) => {
    const { streams } = this.props;

    if ((!streamIds || streamIds.length === 0) && streamIdsWithMissingPermission.length <= 0) {
      return 'No Streams selected, searches in all Streams';
    }

    const warning = streamIdsWithMissingPermission.length > 0
      ? <Alert bsStyle="warning">Missing Stream Permissions for:<br />{streamIdsWithMissingPermission.join(', ')}</Alert>
      : null;

    const renderedStreams = streamIds
      .map((id) => streams.find((s) => s.id === id) || id)
      .sort((s1, s2) => naturalSortIgnoreCase(s1.title || s1, s2.title || s2))
      .map(formatStreamOrId);

    return (
      <>
        {warning}
        {renderedStreams}
      </>
    );
  };

  renderSearchFilters = () => {
    const { filters } = this.props.config;

    if (!filters || filters.length === 0) {
      return <dd>No filters configured</dd>;
    }

    return (
      <dd>
        {filters.map((filter) => (
          <div key={filter.id}>
            {filter.title} -&gt; <code>{filter.queryString}</code>
          </div>
        ))}
      </dd>
    );
  };

  render() {
    const { config, currentUser } = this.props;
    const {
      query,
      query_parameters: queryParameters,
      streams,
      search_within_ms: searchWithinMs,
      execute_every_ms: executeEveryMs,
      _is_scheduled: isScheduled,
      event_limit,
      group_by: groupBy,
      series,
      conditions,
    } = config;

    const conditionType = getConditionType(config);

    const searchWithin = extractDurationAndUnit(searchWithinMs, TIME_UNITS);
    const executeEvery = extractDurationAndUnit(executeEveryMs, TIME_UNITS);

    const effectiveStreamIds = streams?.filter((s) => isPermitted(currentUser.permissions, `streams:read:${s}`));
    const streamIdsWithMissingPermission = streams?.filter((s) => !effectiveStreamIds.includes(s));

    const validationResults = validateExpression(conditions.expression, series);

    return (
      <dl>
        <dt>Type</dt>
        <dd>{upperFirst(conditionType)}</dd>
        <dt>Search Query</dt>
        <dd>{query || '*'}</dd>
        {queryParameters.length > 0 && renderQueryParameters(queryParameters)}
        <dt>Search Filters</dt>
        {this.renderSearchFilters()}
        <dt>Streams</dt>
        <dd className={styles.streamList}>{this.renderStreams(effectiveStreamIds, streamIdsWithMissingPermission)}</dd>
        <dt>Search within</dt>
        <dd>{searchWithin.duration} {searchWithin.unit.toLowerCase()}</dd>
        <dt>Execute search every</dt>
        <dd>{executeEvery.duration} {executeEvery.unit.toLowerCase()}</dd>
        <dt>Enable scheduling</dt>
        <dd>{isScheduled ? 'yes' : 'no'}</dd>
        {conditionType === 'filter' && (
          <>
            <dt>Event limit</dt>
            <dd>{event_limit}</dd>
          </>
        )}
        {conditionType === 'aggregation' && (
          <>
            <dt>Group by Field(s)</dt>
            <dd>{groupBy && groupBy.length > 0 ? groupBy.join(', ') : 'No Group by configured'}</dd>
            <dt>Create Events if</dt>
            <dd>
              {validationResults.isValid
                ? <AggregationConditionSummary series={series} conditions={conditions} />
                : (
                  <Alert bsSize="small" bsStyle="danger">
                    Condition is not valid: {validationResults.errors.join(', ')}
                  </Alert>
                )}
            </dd>
          </>
        )}
        <dt>Actions</dt>
        <dd>
          <LinkToReplaySearch />
        </dd>
      </dl>
    );
  }
}

export default withStreams(FilterAggregationSummary);
