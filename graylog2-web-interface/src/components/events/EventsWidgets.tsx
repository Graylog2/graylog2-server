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
import styled from 'styled-components';

import type { MiddleSectionProps } from 'components/common/PaginatedEntityTable/PaginatedEntityTable';
import usePluginEntities from 'hooks/usePluginEntities';
import EventsMetrics from 'components/events/EventsMetrics';
import EventsHistogram from 'components/events/EventsHistogram';

const Container = styled.div`
  position: relative;
`;
const FloatRight = styled.div`
  position: absolute;
  right: 10px;
`;

const EventsActions = ({ searchParams, setFilters }: MiddleSectionProps) => {
  const eventsMetricsActions = usePluginEntities('events.metrics.actions');

  const actions = eventsMetricsActions.map(({ id, component: Component }) => (
    <Component key={`events-metrics-action-${id}`} searchParams={searchParams} setFilters={setFilters} />
  ));

  return (
    <Container>
      <FloatRight>{actions}</FloatRight>
    </Container>
  );
};

const EventsWidgets = ({ searchParams, setFilters }: MiddleSectionProps) => (
  <EventsMetrics>
    <EventsActions searchParams={searchParams} setFilters={setFilters} />
    <EventsHistogram searchParams={searchParams} setFilters={setFilters} />
  </EventsMetrics>
);
export default EventsWidgets;
