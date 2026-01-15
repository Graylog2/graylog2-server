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
import { useState } from 'react';
import styled from 'styled-components';

import useSelectedEvents from 'components/events/bulk-replay/useSelectedEvents';
import ReplaySearch from 'components/events/ReplaySearch';
import type { Event } from 'components/events/events/types';
import Center from 'components/common/Center';
import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';
import EventReplaySelectedProvider from 'contexts/EventReplaySelectedProvider';
import { Alert } from 'components/bootstrap';
import type { ResolutionState } from 'contexts/EventReplaySelectedContext';

const Container = styled.div`
  display: flex;
  height: 100%;
`;

const ReplayedSearchContainer = styled.div`
  width: 100%;
  overflow: auto;
  padding: 5px;
`;

type Props = {
  initialEventIds: Array<string>;
  events: { [eventId: string]: { event: Event } };
};

const searchPageLayout: Partial<LayoutState> = {
  sidebar: {
    isShown: false,
  },
  synchronizeUrl: false,
} as const;

const AlertWrapper = ({ children = null }: React.PropsWithChildren) => (
  <Alert bsStyle="info">
    <Center>{children}</Center>
  </Alert>
);

const InfoAlert = ({
  eventIds,
  selectedEvent,
}: {
  eventIds: Array<ResolutionState>;
  selectedEvent: { event: Event };
}) => {
  const total = eventIds.length;
  const completed = eventIds.filter((event) => event.status === 'DONE').length;

  if (total === 0) {
    return (
      <AlertWrapper>
        You have removed all events from the list. You can now return back by clicking the &ldquo;Close&rdquo; button.
      </AlertWrapper>
    );
  }

  if (!selectedEvent && total === completed) {
    return (
      <AlertWrapper>
        You are done reviewing all events. You can now select a bulk action to apply to all remaining events, or close
        the page to return to the events list.
      </AlertWrapper>
    );
  }

  if (!selectedEvent) {
    return (
      <AlertWrapper>
        You have no event selected. Please select an event from the list to replay its search.
      </AlertWrapper>
    );
  }

  return null;
};

const ReplayedSearch = ({
  events: _events,
}: React.PropsWithChildren<{
  events: Props['events'];
}>) => {
  const [events] = useState<Props['events']>(_events);
  const { eventIds, selectedId } = useSelectedEvents();
  const selectedEvent = events?.[selectedId];

  return (
    <>
      <InfoAlert eventIds={eventIds} selectedEvent={selectedEvent} />
      <ReplaySearch
        key={`replaying-search-for-event-${selectedEvent?.event?.id}`}
        alertId={selectedEvent?.event?.id}
        definitionId={selectedEvent?.event?.event_definition_id}
        searchPageLayout={searchPageLayout}
      />
    </>
  );
};

const BulkEventReplay = ({ initialEventIds, events }: Props) => (
  <EventReplaySelectedProvider initialEventIds={initialEventIds}>
    <Container>
      <ReplayedSearchContainer>
        <ReplayedSearch events={events} />
      </ReplayedSearchContainer>
    </Container>
  </EventReplaySelectedProvider>
);

export default BulkEventReplay;
