import * as React from 'react';
import { useState } from 'react';
import styled, { css } from 'styled-components';

import EventListItem from 'components/events/bulk-replay/EventListItem';
import useSelectedEvents from 'components/events/bulk-replay/useSelectedEvents';
import ReplaySearch from 'components/events/bulk-replay/ReplaySearch';
import type { Event } from 'components/events/events/types';

const Container = styled.div`
  display: flex;
  height: 100%;
`;

const EventsListSidebar = styled.div(({ theme }) => css`
  flex-shrink: 0;
  position: relative;
  width: 25vw;
  height: 100%;
  top: 0;
  left: 0;
  overflow: auto;
  padding: 5px 10px;

  background: ${theme.colors.global.contentBackground};
  border-right: none;
  box-shadow: 3px 3px 3px ${theme.colors.global.navigationBoxShadow};

  z-index: 1030;
`);

const ReplayedSearchContainer = styled.div`
  width: 100%;
  overflow: auto;
  padding: 5px;
`;

const StyledList = styled.ul`
  padding-inline-start: 0;
  margin-top: 20px;
`;

type Props = {
  initialEventIds: Array<string>;
  events: { [eventId: string]: { event: Event } };
}

const BulkEventReplay = ({ initialEventIds, events: _events }: Props) => {
  const [events] = useState<Props['events']>(_events);
  const { eventIds, selectedId, removeItem, selectItem, markItemAsDone } = useSelectedEvents(initialEventIds);
  const selectedEvent = events?.[selectedId];
  const total = eventIds.length;
  const completed = eventIds.filter((event) => event.status === 'DONE').length;

  return (
    <Container>
      <EventsListSidebar>
        <h3>Replay Search</h3>
        <p>
          The following list contains all of the events/alerts you selected in the previous step, allowing you to
          investigate the replayed search for each of them.
        </p>
        <i>Investigation of {completed}/{total} events completed.</i>
        <StyledList>
          {eventIds.map(({ id: eventId, status }) => (
            <EventListItem event={events?.[eventId]?.event}
                           selected={eventId === selectedId}
                           done={status === 'DONE'}
                           removeItem={removeItem}
                           onClick={() => selectItem(eventId)}
                           markItemAsDone={markItemAsDone} />
          ))}
        </StyledList>
      </EventsListSidebar>
      <ReplayedSearchContainer>
        {selectedEvent
          ? (
            <ReplaySearch key={`replaying-search-for-event-${selectedEvent.event.id}`} event={selectedEvent.event} />
          )
          : <span>You are done investigating all events. You can now select a bulk action to apply to all remaining events, or close the page to return to the events list.</span>}
      </ReplayedSearchContainer>
    </Container>
  );
};

export default BulkEventReplay;
