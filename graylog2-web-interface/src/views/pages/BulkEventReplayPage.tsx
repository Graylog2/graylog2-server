import * as React from 'react';
import { useCallback, useReducer } from 'react';
import { useQuery } from '@tanstack/react-query';
import styled, { css } from 'styled-components';

import { Events } from '@graylog/server-api';

import useLocation from 'routing/useLocation';
import Spinner from 'components/common/Spinner';
import assertUnreachable from 'logic/assertUnreachable';

export type BulkEventReplayState = {
  eventIds: Array<string>;
}

const Container = styled.div`
  display: flex;
  height: 100%;
`;

const EventsListSidebar = styled.div(({ theme }) => css`
  position: relative;
  width: 25vw;
  height: 100%;
  top: 0;
  left: 0;
  overflow: auto;
  padding: 5px;

  background: ${theme.colors.global.contentBackground};
  border-right: none;
  box-shadow: 3px 3px 3px ${theme.colors.global.navigationBoxShadow};

  z-index: 1030;
`);

const ReplayedSearchContainer = styled.div`
  overflow: auto;
  padding: 5px;
`;

const useEventsById = (eventIds: Array<string>) => useQuery(['events', eventIds], () => Events.getByIds({ event_ids: eventIds }));
type Event = {
  id: string,
  message: string,
}

type EventListItemProps = {
  event: Event,
  done: boolean,
  selected: boolean,
  onClick: () => void,
  removeItem: (id: string) => void,
  markItemAsDone: (id: string) => void,
}
const EventListItem = ({ event, onClick, selected, removeItem, markItemAsDone }: EventListItemProps) => (
  <li key={`event-replay-list-${event?.id}`}>
    {selected ? '-' : ''}
    <a onClick={onClick}>{event?.message ?? <i>Unknown</i>}</a>

    <button onClick={() => removeItem(event.id)}>x</button>
    <button onClick={() => markItemAsDone(event.id)}>done</button>
  </li>
);

type ResolutionState = { id: string, status: 'OPEN' | 'DONE' };

type Action = {
  type: 'remove' | 'done' | 'select',
  id: string,
}
type State = {
  selectedId: string,
  eventIds: Array<ResolutionState>,
}

const pickNextId = (eventIds: Array<ResolutionState>) => eventIds.find((event) => event.status === 'OPEN')?.id;

const createInitialState = (_eventIds: Array<string>) => {
  const eventIds = _eventIds.map((id) => ({ id, status: 'OPEN' } as const));
  const selectedId = pickNextId(eventIds);

  return {
    selectedId,
    eventIds,
  };
};

const reducer = (state: State, action: Action) => {
  if (action.type === 'remove') {
    const eventIds = state.eventIds.filter((event) => event.id !== action.id);
    const selectedId = state.selectedId === action.id
      ? pickNextId(eventIds)
      : state.selectedId;

    return {
      selectedId,
      eventIds,
    };
  }

  if (action.type === 'done') {
    const eventIds = state.eventIds.map((event) => (event.id === action.id
      ? { id: action.id, status: 'DONE' } as const
      : event));
    const selectedId = state.selectedId === action.id
      ? pickNextId(eventIds)
      : state.selectedId;

    return {
      selectedId,
      eventIds,
    };
  }

  if (action.type === 'select') {
    return {
      ...state,
      selectedId: action.id,
    };
  }

  return assertUnreachable(action.type, `Invalid action dispatched: ${action}`);
};

const useSelectedEvents = (eventIds: Array<string>) => {
  const [state, dispatch] = useReducer(reducer, createInitialState(eventIds));
  const removeItem = useCallback((eventId: string) => dispatch({ type: 'remove', id: eventId }), []);
  const markItemAsDone = useCallback((eventId: string) => dispatch({ type: 'done', id: eventId }), []);
  const selectItem = useCallback((eventId: string) => dispatch({ type: 'select', id: eventId }), []);

  return {
    ...state,
    removeItem,
    markItemAsDone,
    selectItem,
  };
};

const BulkEventReplayPage = () => {
  const location = useLocation<BulkEventReplayState>();
  const initialEventIds = location.state.eventIds ?? [];
  const { eventIds, selectedId, removeItem, selectItem, markItemAsDone } = useSelectedEvents(initialEventIds);
  const { data: events, isInitialLoading } = useEventsById(initialEventIds);
  const selectedEvent = events?.[selectedId];

  return isInitialLoading
    ? <Spinner />
    : (
      <Container>
        <EventsListSidebar>
          <h3>Replay Search</h3>
          <p>
            The following list contains all of the events/alerts you selected in the previous step, allowing you to
            investigate the replayed search for each of them.
          </p>
          <ul>
            {eventIds.map(({ id: eventId, status }) => (
              <EventListItem event={events?.[eventId]?.event}
                             selected={eventId === selectedId}
                             done={status === 'DONE'}
                             removeItem={removeItem}
                             onClick={() => selectItem(eventId)}
                             markItemAsDone={markItemAsDone} />
            ))}
          </ul>
        </EventsListSidebar>
        <ReplayedSearchContainer>
          {selectedEvent
            ? (
              <span>
                Replayed Search for {selectedEvent.event.id}: {selectedEvent.event.message}
              </span>
            )
            : <span>You are done investigating all events. You can now select a bulk action to apply to all remaining events, or close the page to return to the events list.</span>}
        </ReplayedSearchContainer>
      </Container>
    );
};

export default BulkEventReplayPage;
