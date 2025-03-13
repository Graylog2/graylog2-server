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
