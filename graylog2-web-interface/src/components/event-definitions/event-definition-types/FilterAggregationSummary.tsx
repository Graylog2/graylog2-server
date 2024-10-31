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
import * as React from 'react';
import { useContext } from 'react';
import isEmpty from 'lodash/isEmpty';
import upperFirst from 'lodash/upperFirst';

import { describeExpression } from 'util/CronUtils';
import { Link } from 'components/common/router';
import { Alert } from 'components/bootstrap';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import { isPermitted } from 'util/PermissionsMixin';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import Routes from 'routing/Routes';
import validateExpression from 'logic/alerts/AggregationExpressionValidation';
import type { Stream } from 'views/stores/StreamsStore';
import type User from 'logic/users/User';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import type { LookupTableParameterJson } from 'views/logic/parameters/LookupTableParameter';
import StreamsContext from 'contexts/StreamsContext';

import AggregationConditionSummary from './AggregationConditionSummary';
import { TIME_UNITS } from './FilterForm';
import styles from './FilterAggregationSummary.css';

import LinkToReplaySearch from '../replay-search/LinkToReplaySearch';

const StreamOrId = ({ streamOrId }: { streamOrId: Stream | string }) => {
  if (typeof streamOrId === 'string') {
    return <span key={streamOrId}><em>{streamOrId}</em></span>;
  }

  return (
    <span key={streamOrId.id}>
      <Link to={Routes.stream_search(streamOrId.id)}>{streamOrId.title}</Link>
    </span>
  );
};

const getConditionType = (config: EventDefinition['config']) => {
  const { group_by: groupBy, series, conditions } = config;

  return (isEmpty(groupBy)
    && (!conditions || isEmpty(conditions) || conditions.expression === null)
    && isEmpty(series)
    ? 'filter' : 'aggregation');
};

const QueryParameters = ({ queryParameters }: { queryParameters: Array<LookupTableParameterJson & { embryonic?: boolean }> }) => {
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

type Props = {
  streams: Array<Stream>,
  config: EventDefinition['config'],
  currentUser: User,
  definitionId?: string,
}

const SearchFilters = ({ filters }: { filters: EventDefinition['config']['filters'] }) => {
  if (!filters || filters.length === 0) {
    return <dd>No filters configured</dd>;
  }

  return (
    <dd>
      {filters.map((filter) => (
        <div key={filter.id}>
          {filter.title ? `${filter.title} -> ` : null}<code>{filter.queryString}</code>
        </div>
      ))}
    </dd>
  );
};

type StreamsProps = {
  streams: Array<Stream>,
  streamIds: Array<string>,
  streamIdsWithMissingPermission: Array<string>,
}

const Streams = ({ streams, streamIds, streamIdsWithMissingPermission }: StreamsProps) => {
  if ((!streamIds || streamIds.length === 0) && streamIdsWithMissingPermission.length <= 0) {
    return <>No Streams selected, searches in all Streams</>;
  }

  const warning = streamIdsWithMissingPermission.length > 0
    ? <Alert bsStyle="warning">Missing Stream Permissions for:<br />{streamIdsWithMissingPermission.join(', ')}</Alert>
    : null;

  const renderedStreams = streamIds
    .map((id) => streams.find((s) => s.id === id) || id)
    .sort((s1, s2) => naturalSortIgnoreCase(typeof s1 === 'object' ? s1.title : s1, typeof s2 === 'object' ? s2.title : s2))
    .map((s) => <StreamOrId streamOrId={s} />);

  return (
    <>
      {warning}
      {renderedStreams}
    </>
  );
};

const FilterAggregationSummary = ({ config, currentUser, definitionId }: Props) => {
  const streams = useContext(StreamsContext);
  const {
    query,
    query_parameters: queryParameters,
    streams: configStreams,
    stream_categories: streamCategories,
    search_within_ms: searchWithinMs,
    execute_every_ms: executeEveryMs,
    use_cron_scheduling: useCronScheduling,
    cron_expression: cronExpression,
    cron_timezone: cronTimezone,
    _is_scheduled: isScheduled,
    event_limit,
    group_by: groupBy,
    series,
    conditions,
  } = config;

  const conditionType = getConditionType(config);

  const searchWithin = extractDurationAndUnit(searchWithinMs, TIME_UNITS);
  const executeEvery = extractDurationAndUnit(executeEveryMs, TIME_UNITS);

  const effectiveStreamIds = configStreams?.filter((s) => isPermitted(currentUser.permissions, `streams:read:${s}`));
  const streamIdsWithMissingPermission = configStreams?.filter((s) => !effectiveStreamIds.includes(s));

  const validationResults = validateExpression(conditions.expression, series);

  const renderCronExpression = (expression) => {
    if (expression) {
      return describeExpression(expression);
    }

    return 'Error: no cron expression specified!';
  };

  const renderStreamCategories = () => {
    if (!streamCategories || streamCategories.length === 0) return null;

    const renderedCategories = streamCategories.map((s) => <StreamOrId streamOrId={s} />);

    return (
      <>
        <dt>Stream Categories</dt>
        <dd className={styles.streamList}>{renderedCategories}</dd>
      </>
    );
  };

  return (
    <dl>
      <dt>Type</dt>
      <dd>{upperFirst(conditionType)}</dd>
      <dt>Search Query</dt>
      <dd>{query || '*'}</dd>
      {queryParameters.length > 0 && <QueryParameters queryParameters={queryParameters} />}
      <dt>Search Filters</dt>
      <SearchFilters filters={config.filters} />
      <dt>Streams</dt>
      <dd className={styles.streamList}><Streams streams={streams} streamIds={effectiveStreamIds} streamIdsWithMissingPermission={streamIdsWithMissingPermission} /></dd>
      {renderStreamCategories()}
      <dt>Search within</dt>
      <dd>{searchWithin.duration} {searchWithin.unit.toLowerCase()}</dd>
      <dt>Use Cron Scheduling</dt>
      <dd>{useCronScheduling ? 'yes' : 'no'}</dd>
      {useCronScheduling
        ? (
          <>
            <dt>Cron Expression</dt>
            <dd>{cronExpression}</dd>
            <dt>Cron Description</dt>
            <dd>{renderCronExpression(cronExpression)}</dd>
            <dt>Time Zone</dt>
            <dd>{cronTimezone}</dd>
          </>
        )
        : (
          <>
            <dt>Execute search every</dt>
            <dd>{executeEvery.duration} {executeEvery.unit.toLowerCase()}</dd>
          </>
        )}
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
                <Alert bsStyle="danger">
                  Condition is not valid: {validationResults.errors.join(', ')}
                </Alert>
              )}
          </dd>
        </>
      )}
      <dt>Actions</dt>
      <dd>
        <LinkToReplaySearch id={definitionId} />
      </dd>
    </dl>
  );
};

export default FilterAggregationSummary;
