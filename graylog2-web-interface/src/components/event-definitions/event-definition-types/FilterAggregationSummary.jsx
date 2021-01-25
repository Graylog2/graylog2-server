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
import lodash from 'lodash';

import { Link } from 'components/graylog/router';
import { Alert } from 'components/graylog';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import { Icon } from 'components/common';
import { isPermitted } from 'util/PermissionsMixin';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import Routes from 'routing/Routes';
import validateExpression from 'logic/alerts/AggregationExpressionValidation';

import AggregationConditionSummary from './AggregationConditionSummary';
import withStreams from './withStreams';
import { TIME_UNITS } from './FilterForm';
import styles from './FilterAggregationSummary.css';

class FilterAggregationSummary extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    streams: PropTypes.array.isRequired,
  };

  getConditionType = (config) => {
    const { group_by: groupBy, series, conditions } = config;

    return (lodash.isEmpty(groupBy)
    && (!conditions || lodash.isEmpty(conditions) || conditions.expression === null)
    && lodash.isEmpty(series)
      ? 'filter' : 'aggregation');
  };

  formatStreamOrId = (streamOrId) => {
    if (typeof streamOrId === 'string') {
      return <span key={streamOrId}><em>{streamOrId}</em></span>;
    }

    return (
      <span key={streamOrId.id}>
        <Link to={Routes.stream_search(streamOrId.id)}>{streamOrId.title}</Link>
      </span>
    );
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
      .map(this.formatStreamOrId);

    return (
      <>
        {warning}
        {renderedStreams}
      </>
    );
  };

  renderQueryParameters = (queryParameters) => {
    if (queryParameters.some((p) => p.embryonic)) {
      const undeclaredParameters = queryParameters.filter((p) => p.embryonic)
        .map((p) => p.name)
        .join(', ');

      return (
        <Alert bsStyle="danger">
          <Icon name="exclamation-triangle" />&nbsp;There are undeclared query parameters: {undeclaredParameters}
        </Alert>
      );
    }

    return <dd>{queryParameters.map((p) => p.name).join(', ')}</dd>;
  }

  render() {
    const { config, currentUser } = this.props;
    const {
      query,
      query_parameters: queryParameters,
      streams,
      search_within_ms: searchWithinMs,
      execute_every_ms: executeEveryMs,
      _is_scheduled: isScheduled,
      group_by: groupBy,
      series,
      conditions,
    } = config;

    const conditionType = this.getConditionType(config);

    const searchWithin = extractDurationAndUnit(searchWithinMs, TIME_UNITS);
    const executeEvery = extractDurationAndUnit(executeEveryMs, TIME_UNITS);

    const effectiveStreamIds = streams.filter((s) => isPermitted(currentUser.permissions, `streams:read:${s}`));
    const streamIdsWithMissingPermission = streams.filter((s) => !effectiveStreamIds.includes(s));

    const validationResults = validateExpression(conditions.expression, series);

    return (
      <dl>
        <dt>Type</dt>
        <dd>{lodash.upperFirst(conditionType)}</dd>
        <dt>Search Query</dt>
        <dd>{query || '*'}</dd>
        {queryParameters.length > 0 && this.renderQueryParameters(queryParameters)}
        <dt>Streams</dt>
        <dd className={styles.streamList}>{this.renderStreams(effectiveStreamIds, streamIdsWithMissingPermission)}</dd>
        <dt>Search within</dt>
        <dd>{searchWithin.duration} {searchWithin.unit.toLowerCase()}</dd>
        <dt>Execute search every</dt>
        <dd>{executeEvery.duration} {executeEvery.unit.toLowerCase()}</dd>
        <dt>Enable scheduling</dt>
        <dd>{isScheduled ? 'yes' : 'no'}</dd>
        {conditionType === 'aggregation' && (
          <>
            <dt>Group by Field(s)</dt>
            <dd>{groupBy && groupBy.length > 0 ? groupBy.join(', ') : 'No Group by configured'}</dd>
            <dt>Create Events if</dt>
            <dd>
              {validationResults.isValid
                ? <AggregationConditionSummary series={series} conditions={conditions} />
                : (
                  <Alert bsSize="small" bsStyle="danger"><Icon name="exclamation-triangle" />&nbsp;
                    Condition is not valid: {validationResults.errors.join(', ')}
                  </Alert>
                )}
            </dd>
          </>
        )}
      </dl>
    );
  }
}

export default withStreams(FilterAggregationSummary);
