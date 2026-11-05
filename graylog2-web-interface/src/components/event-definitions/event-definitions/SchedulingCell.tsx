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
import styled, { css } from 'styled-components';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import i18n from 'i18n';
import { describeExpression } from 'util/CronUtils';
import { OverlayTrigger, Icon, Timestamp } from 'components/common';
import Button from 'components/bootstrap/Button';
import {
  clearNotificationQueue,
  EVENT_DEFINITIONS_QUERY_KEY,
} from 'components/event-definitions/hooks/useEventDefinitions';

import type { Scheduler, EventDefinition } from '../event-definitions-types';
import formatDuration from '../formatDuration';

const t = (key: string, options = {}) => i18n.t(key, { ns: 'event_definitions', ...options });

type Props = {
  definition: EventDefinition;
};

const DetailTitle = styled.dt`
  float: left;
  clear: left;
`;

const DetailValue = styled.dd(
  ({ theme }) => css`
    margin-left: 180px;
    overflow-wrap: break-word;

    &:not(:last-child) {
      border-bottom: 1px solid ${theme.colors.variant.lightest.default};
      margin-bottom: 5px;
      padding-bottom: 5px;
    }
  `,
);

const DetailsOverlayTrigger = styled(OverlayTrigger)`
  vertical-align: bottom;
`;

const getTimeRange = (scheduler: Scheduler) => {
  const from = scheduler?.data?.timerange_from;
  const to = scheduler?.data?.timerange_to;

  return (
    <>
      <DetailTitle>{t('scheduling.next_time_range')}</DetailTitle>
      <DetailValue>
        <Timestamp dateTime={from} /> <Icon name="arrow_circle_right" /> <Timestamp dateTime={to} />
      </DetailValue>
    </>
  );
};

const describeSchedule = (isCron: boolean, value: number | string) => {
  if (isCron) {
    const cronDescription = describeExpression(value as string);

    // Lower case the A in At or the E in Every
    return cronDescription.charAt(0).toLowerCase() + cronDescription.slice(1);
  }

  return t('scheduling.every', { duration: formatDuration(value as number) });
};

const detailsPopover = (scheduler: Scheduler, clearNotifications: () => void) => (
  <dl>
    <DetailTitle>{t('scheduling.status')}</DetailTitle>
    <DetailValue>{scheduler.status}</DetailValue>
    {scheduler.triggered_at && (
      <>
        <DetailTitle>{t('scheduling.last_execution')}</DetailTitle>
        <DetailValue>
          <Timestamp dateTime={scheduler.triggered_at} />
        </DetailValue>
      </>
    )}
    {scheduler.next_time && (
      <>
        <DetailTitle>{t('scheduling.next_execution')}</DetailTitle>
        <DetailValue>
          <Timestamp dateTime={scheduler.next_time} />
        </DetailValue>
      </>
    )}
    {getTimeRange(scheduler)}
    <DetailTitle>{t('scheduling.queued_notifications')}</DetailTitle>
    <DetailValue>
      {scheduler.queued_notifications}
      {scheduler.queued_notifications > 0 && (
        <Button bsStyle="link" bsSize="xsmall" onClick={() => clearNotifications()}>
          {t('scheduling.clear')}
        </Button>
      )}
    </DetailValue>
  </dl>
);

const SchedulingInfo = ({
  executeEveryMs,
  searchWithinMs,
  useCronScheduling,
  cronExpression,
  scheduler,
  title,
  clearNotifications,
}: {
  executeEveryMs: number;
  searchWithinMs: number;
  useCronScheduling: boolean;
  cronExpression: string;
  scheduler: Scheduler;
  title: string;
  clearNotifications: () => void;
}) => {
  const executeEveryFormatted = describeSchedule(
    useCronScheduling,
    useCronScheduling ? cronExpression : executeEveryMs,
  );
  const { t: translate } = useTranslation('event_definitions');
  const description = searchWithinMs
    ? translate('scheduling.runs_searching_within', {
        schedule: executeEveryFormatted,
        duration: formatDuration(searchWithinMs),
      })
    : translate('scheduling.runs', { schedule: executeEveryFormatted });

  return (
    <>
      {`${description} `}
      <DetailsOverlayTrigger
        trigger="click"
        rootClose
        placement="left"
        title={translate('scheduling.details', { title })}
        overlay={detailsPopover(scheduler, clearNotifications)}
        width={500}>
        <Icon name="info" title={translate('scheduling.details', { title })} bsStyle={'info'} />
      </DetailsOverlayTrigger>
    </>
  );
};

const SchedulingCell = ({ definition }: Props) => {
  const { t: translate } = useTranslation('event_definitions');
  const queryClient = useQueryClient();

  if (!definition?.config?.search_within_ms && !definition?.config?.execute_every_ms) {
    return <>{translate('scheduling.not_scheduled')}</>;
  }

  const clearNotifications = (eventDefinition: EventDefinition) => () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(translate('scheduling.clear_confirm', { title: eventDefinition.title }))) {
      clearNotificationQueue(eventDefinition).then(
        () => queryClient.invalidateQueries({ queryKey: EVENT_DEFINITIONS_QUERY_KEY }),
        () => {
          // Error feedback is handled by `clearNotificationQueue` itself.
        },
      );
    }
  };

  const {
    title,
    config: {
      search_within_ms: searchWithinMs,
      execute_every_ms: executeEveryMs,
      use_cron_scheduling: useCronScheduling,
      cron_expression: cronExpression,
    },
    scheduler,
  } = definition;

  return (
    <SchedulingInfo
      executeEveryMs={executeEveryMs}
      searchWithinMs={searchWithinMs}
      useCronScheduling={useCronScheduling}
      cronExpression={cronExpression}
      title={title}
      scheduler={scheduler}
      clearNotifications={clearNotifications(definition)}
    />
  );
};

export default SchedulingCell;
