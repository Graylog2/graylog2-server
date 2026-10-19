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
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import { describeExpression } from 'util/CronUtils';
import { Link } from 'components/common';
import { Alert } from 'components/bootstrap';
import { DataWell } from 'components/lookup-tables/layout-componets';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import usePermissions from 'hooks/usePermissions';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import Routes from 'routing/Routes';
import validateExpression from 'logic/alerts/AggregationExpressionValidation';
import type { Expression } from 'logic/alerts/AggregationExpressionValidation';
import type { Stream } from 'logic/streams/types';
import streamCategoryName from 'logic/streams/streamCategoryName';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import type { ParameterJson } from 'views/logic/parameters/Parameter';
import StreamsContext from 'contexts/StreamsContext';

import AggregationConditionSummary from './AggregationConditionSummary';
import { TIME_UNITS } from './FilterForm';
import styles from './FilterAggregationSummary.css';

import EventDefinitionReplaySearchLink from '../replay-search/EventDefinitionReplaySearchLink';

const StyledDataWell = styled(DataWell)`
  line-height: 1.8;
  white-space: pre;
  font-family: monospace;
  font-size: medium;
  color: ${({ theme }) => (theme.mode === 'light' ? 'darkslateblue' : 'lightsteelblue')};
  overflow: auto;
  text-wrap: auto;
`;

const StreamOrId = ({ streamOrId }: { streamOrId: Stream | string }) => {
  if (typeof streamOrId === 'string') {
    return (
      <span key={streamOrId}>
        <em>{streamOrId}</em>
      </span>
    );
  }

  return (
    <span key={streamOrId.id}>
      <Link to={Routes.stream_search(streamOrId.id)}>{streamOrId.title}</Link>
    </span>
  );
};

const getConditionType = (config: EventDefinition['config']) => {
  const { group_by: groupBy, series, conditions } = config;

  return isEmpty(groupBy) && (!conditions || isEmpty(conditions) || conditions.expression === null) && isEmpty(series)
    ? 'filter'
    : 'aggregation';
};

const QueryParameters = ({ queryParameters }: { queryParameters: Array<ParameterJson & { embryonic?: boolean }> }) => {
  const { t } = useTranslation('event_definitions');

  if (queryParameters.some((p) => p.embryonic)) {
    const undeclaredParameters = queryParameters
      .filter((p) => p.embryonic)
      .map((p) => p.name)
      .join(', ');

    return <Alert bsStyle="danger">{t('summary.undeclared_parameters', { parameters: undeclaredParameters })}</Alert>;
  }

  return <dd>{queryParameters.map((p) => p.name).join(', ')}</dd>;
};

type Props = {
  config: EventDefinition['config'];
  definitionId?: string;
};

const SearchFilters = ({ filters }: { filters: EventDefinition['config']['filters'] }) => {
  const { t } = useTranslation('event_definitions');

  if (!filters || filters?.length === 0) {
    return <dd>{t('summary.no_filters')}</dd>;
  }

  return (
    <dd>
      {filters.map((filter) => (
        <div key={filter.id}>
          {filter.title ? `${filter.title} -> ` : null}
          <code>{filter.queryString}</code>
        </div>
      ))}
    </dd>
  );
};

type StreamsProps = {
  streams: Array<Stream>;
  streamIds: Array<string>;
  streamIdsWithMissingPermission: Array<string>;
};

const Streams = ({ streams, streamIds, streamIdsWithMissingPermission }: StreamsProps) => {
  const { t } = useTranslation('event_definitions');

  if ((!streamIds || streamIds?.length === 0) && streamIdsWithMissingPermission?.length <= 0) {
    return <>{t('summary.no_streams')}</>;
  }

  const warning =
    streamIdsWithMissingPermission?.length > 0 ? (
      <Alert bsStyle="warning">
        {t('summary.missing_stream_permissions')}
        <br />
        {streamIdsWithMissingPermission.join(', ')}
      </Alert>
    ) : null;

  const renderedStreams = streamIds
    .map((id) => streams?.find((s) => s.id === id) ?? id)
    .sort((s1, s2) =>
      naturalSortIgnoreCase(typeof s1 === 'object' ? s1.title : s1, typeof s2 === 'object' ? s2.title : s2),
    )
    .map((s) => <StreamOrId key={typeof s === 'object' ? s.id : s} streamOrId={s} />);

  return (
    <>
      {warning}
      {renderedStreams}
    </>
  );
};

const FilterAggregationSummary = ({ config, definitionId = undefined }: Props) => {
  const { t } = useTranslation('event_definitions');
  const streams = useContext(StreamsContext);
  const { isPermitted } = usePermissions();
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

  const effectiveStreamIds = configStreams?.filter((s) => isPermitted(`streams:read:${s}`));
  const streamIdsWithMissingPermission = configStreams?.filter((s) => !effectiveStreamIds.includes(s));

  const validationResults = validateExpression(conditions?.expression as Expression | null, series);

  const renderCronExpression = (expression) => {
    if (expression) {
      return describeExpression(expression);
    }

    return t('summary.no_cron_expression');
  };

  const renderStreamCategories = () => {
    if (!streamCategories || streamCategories?.length === 0) return null;

    const renderedCategories = streamCategories.map((s) => <StreamOrId key={s} streamOrId={streamCategoryName(s)} />);

    return (
      <>
        <dt>{t('summary.stream_categories')}</dt>
        <dd className={styles.streamList}>{renderedCategories}</dd>
      </>
    );
  };

  return (
    <dl>
      <dt>{t('summary.type')}</dt>
      <dd>{t(`summary.condition_types.${conditionType}`)}</dd>
      <dt>{t('summary.search_query')}</dt>
      <dd>
        <StyledDataWell>{query || '*'}</StyledDataWell>
      </dd>
      {queryParameters?.length > 0 && <QueryParameters queryParameters={queryParameters} />}
      <dt>{t('summary.search_filters')}</dt>
      <SearchFilters filters={config.filters} />
      <dt>{t('summary.streams')}</dt>
      <dd className={styles.streamList}>
        <Streams
          streams={streams}
          streamIds={effectiveStreamIds}
          streamIdsWithMissingPermission={streamIdsWithMissingPermission}
        />
      </dd>
      {renderStreamCategories()}
      <dt>{t('summary.search_within')}</dt>
      <dd>{t(`common:duration.${searchWithin.unit.toLowerCase()}`, { count: searchWithin.duration })}</dd>
      <dt>{t('summary.use_cron')}</dt>
      <dd>{useCronScheduling ? t('summary.yes') : t('summary.no')}</dd>
      {useCronScheduling ? (
        <>
          <dt>{t('summary.cron_expression')}</dt>
          <dd>{cronExpression}</dd>
          <dt>{t('summary.cron_description')}</dt>
          <dd>{renderCronExpression(cronExpression)}</dd>
          <dt>{t('summary.time_zone')}</dt>
          <dd>{cronTimezone}</dd>
        </>
      ) : (
        <>
          <dt>{t('summary.execute_every')}</dt>
          <dd>{t(`common:duration.${executeEvery.unit.toLowerCase()}`, { count: executeEvery.duration })}</dd>
        </>
      )}
      <dt>{t('summary.enable_scheduling')}</dt>
      <dd>{isScheduled ? t('summary.yes') : t('summary.no')}</dd>
      {conditionType === 'filter' && (
        <>
          <dt>{t('summary.event_limit')}</dt>
          <dd>{event_limit}</dd>
        </>
      )}
      {conditionType === 'aggregation' && (
        <>
          <dt>{t('summary.group_by')}</dt>
          <dd>{groupBy && groupBy?.length > 0 ? groupBy.join(', ') : t('summary.no_group_by')}</dd>
          <dt>{t('summary.create_events_if')}</dt>
          <dd>
            {validationResults.isValid ? (
              <AggregationConditionSummary series={series} conditions={conditions} />
            ) : (
              <Alert bsStyle="danger">
                {t('summary.condition_invalid', { errors: validationResults.errors.join(', ') })}
              </Alert>
            )}
          </dd>
        </>
      )}
      <dt>{t('summary.actions')}</dt>
      <dd>
        <EventDefinitionReplaySearchLink eventDefinitionId={definitionId} />
      </dd>
    </dl>
  );
};

export default FilterAggregationSummary;
