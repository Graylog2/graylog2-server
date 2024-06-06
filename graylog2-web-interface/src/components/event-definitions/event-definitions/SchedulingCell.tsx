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

import { OverlayTrigger, Icon, Timestamp } from 'components/common';
import Button from 'components/bootstrap/Button';
import { EventDefinitionsActions } from 'stores/event-definitions/EventDefinitionsStore';

import type { Scheduler, EventDefinition } from '../event-definitions-types';

type Props = {
  definition: EventDefinition,
};

const DetailTitle = styled.dt`
  float: left;
  clear: left;
`;

const DetailValue = styled.dd(({ theme }) => css`
  margin-left: 180px;
  word-wrap: break-word;

  &:not(:last-child) {
    border-bottom: 1px solid ${theme.colors.variant.lightest.default};
    margin-bottom: 5px;
    padding-bottom: 5px;
  }
`);

const DetailsButton = styled(Button)`
  padding: 6px 8px;
`;

const getTimeRange = (scheduler: Scheduler) => {
  const from = scheduler?.data?.timerange_from;
  const to = scheduler?.data?.timerange_to;

  return (
    <>
      <DetailTitle>Next time range:</DetailTitle>
      <DetailValue><Timestamp dateTime={from} /> <Icon name="arrow_circle_right" /> <Timestamp dateTime={to} /></DetailValue>
    </>
  );
};

const detailsPopover = (scheduler: Scheduler, clearNotifications: () => void) => (
  <dl>
    <DetailTitle>Status:</DetailTitle>
    <DetailValue>{scheduler.status}</DetailValue>
    {scheduler.triggered_at && (
      <>
        <DetailTitle>Last execution:</DetailTitle>
        <DetailValue><Timestamp dateTime={scheduler.triggered_at} /></DetailValue>
      </>
    )}
    {scheduler.next_time && (
      <>
        <DetailTitle>Next execution:</DetailTitle>
        <DetailValue><Timestamp dateTime={scheduler.next_time} /></DetailValue>
      </>
    )}
    {getTimeRange(scheduler)}
    <DetailTitle>Queued notifications:</DetailTitle>
    <DetailValue>{scheduler.queued_notifications}
      {scheduler.queued_notifications > 0 && (
        <Button bsStyle="link" bsSize="xsmall" onClick={() => clearNotifications()}>
          clear
        </Button>
      )}
    </DetailValue>
  </dl>
);

const SchedulingInfo = ({
  scheduler,
  title,
  clearNotifications,
  scheduleDescription,
}:{
  scheduler: Scheduler, title: string,
  clearNotifications: () => void,
  scheduleDescription: string,
}) => (
  <>
    {scheduleDescription}
    <OverlayTrigger trigger="click"
                    rootClose
                    placement="left"
                    title={`${title} details.`}
                    overlay={detailsPopover(scheduler, clearNotifications)}
                    width={500}>
      <DetailsButton bsStyle="link"><Icon name="info" /></DetailsButton>
    </OverlayTrigger>
  </>
);

const SchedulingCell = ({ definition } : Props) => {
  if (!definition?.config?.search_within_ms && !definition?.config?.execute_every_ms) {
    return <>Not Scheduled.</>;
  }

  const clearNotifications = (eventDefinition: EventDefinition) => () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to clear queued notifications for "${eventDefinition.title}"?`)) {
      EventDefinitionsActions.clearNotificationQueue(eventDefinition);
    }
  };

  const {
    title,
    scheduler,
    schedule_description: scheduleDescription,
  } = definition;

  return (
    <SchedulingInfo title={title}
                    scheduler={scheduler}
                    scheduleDescription={scheduleDescription}
                    clearNotifications={clearNotifications(definition)} />
  );
};

export default SchedulingCell;
