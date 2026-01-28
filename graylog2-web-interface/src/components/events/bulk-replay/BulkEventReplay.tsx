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
import type { Event } from 'components/events/events/types';
import Center from 'components/common/Center';
import EventReplaySelectedProvider from 'contexts/EventReplaySelectedProvider';
import { Alert } from 'components/bootstrap';
import type { ResolutionState } from 'contexts/EventReplaySelectedContext';
import type { SidebarSection } from 'views/components/sidebar/sidebarSections';
import sidebarSections from 'views/components/sidebar/sidebarSections';
import ReplaySearchSidebar from 'components/events/ReplaySearchSidebar/ReplaySearchSidebar';
import ReplayEventIdRenderer from 'components/events/bulk-replay/NewBulkEventReplay';
import EventReplaySearch from 'components/events/EventReplaySearch';
import useEventDefinition from 'hooks/useEventDefinition';

const Container = styled.div`
  display: flex;
  height: 100%;
`;

const ReplayedSearchContainer = styled.div`
  width: 100%;
  display: flex;
  flex-direction: column;
`;

type Props = {
  initialEventIds: Array<string>;
  events: { [eventId: string]: { event: Event } };
};

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

  if (total === 0) {
    return (
      <AlertWrapper>
        You have removed all events from the list. You can now return back by clicking the &ldquo;Close&rdquo; button.
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

const replaySection: SidebarSection = {
  key: 'eventDescription',
  title: null,
  hoverTitle: 'Replayed Search',
  icon: 'play_arrow',
  content: ReplaySearchSidebar,
};

const searchPageLayout = {
  sidebar: {
    isShown: true,
    title: ReplayEventIdRenderer,
    sections: [replaySection, ...sidebarSections],
    contentColumnWidth: 350,
  },
};

const ReplayedSearch = ({
  events: _events,
}: React.PropsWithChildren<{
  events: Props['events'];
}>) => {
  const [events] = useState<Props['events']>(_events);
  const { eventIds, selectedId } = useSelectedEvents();
  const selectedEvent = events?.[selectedId];
  const { data: eventDefinitionMappedData } = useEventDefinition(selectedEvent?.event?.event_definition_id);

  return (
    <>
      <InfoAlert eventIds={eventIds} selectedEvent={selectedEvent} />
      <EventReplaySearch
        eventData={selectedEvent.event}
        eventDefinitionMappedData={eventDefinitionMappedData}
        searchPageLayout={searchPageLayout}
        forceSidebarPinned
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
