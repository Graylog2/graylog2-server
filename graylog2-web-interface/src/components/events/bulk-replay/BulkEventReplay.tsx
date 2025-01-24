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
import styled, { css } from 'styled-components';

import EventListItem from 'components/events/bulk-replay/EventListItem';
import useSelectedEvents from 'components/events/bulk-replay/useSelectedEvents';
import ReplaySearch from 'components/events/ReplaySearch';
import type { Event } from 'components/events/events/types';
import Button from 'components/bootstrap/Button';
import DropdownButton from 'components/bootstrap/DropdownButton';
import useEventBulkActions from 'components/events/events/hooks/useEventBulkActions';
import Center from 'components/common/Center';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';

const Container = styled.div`
  display: flex;
  height: 100%;
`;

const EventsListSidebar = styled.div(({ theme }) => css`
  display: flex;
  flex-direction: column;
  
  flex-shrink: 0;
  position: relative;
  width: 20vw;
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
  flex-grow: 1;
  padding-inline-start: 0;
  margin-top: 20px;
`;

const ActionsBar = styled(ButtonToolbar)`
  align-self: flex-end;
  display: flex;
  justify-content: flex-end;
  align-items: end;
  gap: 0.25em;
`;

type Props = {
  initialEventIds: Array<string>;
  events: { [eventId: string]: { event: Event } };
  onClose: () => void;
}

type RemainingBulkActionsProps = {
  events: Event[];
  completed: boolean;
}

const RemainingBulkActions = ({ completed, events }: RemainingBulkActionsProps) => {
  const { actions, pluggableActionModals } = useEventBulkActions(events);

  return (
    <>
      <DropdownButton title="Bulk actions"
                      bsStyle={completed ? 'success' : 'default'}
                      id="bulk-actions-dropdown"
                      disabled={!events?.length}>
        {actions}
      </DropdownButton>
      {pluggableActionModals}
    </>
  );
};

const searchPageLayout: Partial<LayoutState> = {
  sidebar: {
    isShown: false,
  },
  synchronizeUrl: false,
} as const;

const ReplayedSearch = ({ total, completed, selectedEvent }: React.PropsWithChildren<{
  total: number;
  completed: number;
  selectedEvent: { event: Event } | undefined;
}>) => {
  if (total === 0) {
    return (
      <Center>
        You have removed all events from the list. You can now return back by clicking the &ldquo;Close&rdquo; button.
      </Center>
    );
  }

  if (!selectedEvent && total === completed) {
    return (
      <Center>
        You are done investigating all events. You can now select a bulk action to apply to all remaining events, or close the page to return to the events list.
      </Center>
    );
  }

  if (!selectedEvent) {
    return (
      <Center>
        You have no event selected. Please select an event from the list to replay its search.
      </Center>
    );
  }

  return (
    <ReplaySearch key={`replaying-search-for-event-${selectedEvent.event.id}`}
                  alertId={selectedEvent.event.id}
                  definitionId={selectedEvent.event.event_definition_id}
                  searchPageLayout={searchPageLayout} />
  );
};

const Headline = styled.h2`
  margin-bottom: 10px;
`;

const BulkEventReplay = ({ initialEventIds, events: _events, onClose }: Props) => {
  const [events] = useState<Props['events']>(_events);
  const { eventIds, selectedId, removeItem, selectItem, markItemAsDone } = useSelectedEvents(initialEventIds);
  const selectedEvent = events?.[selectedId];
  const total = eventIds.length;
  const completed = eventIds.filter((event) => event.status === 'DONE').length;
  const remainingEvents = eventIds.map((eventId) => events[eventId.id]?.event);

  return (
    <Container>
      <EventsListSidebar>
        <Headline>Replay Search</Headline>
        <p>
          The following list contains all of the events/alerts you selected in the previous step, allowing you to
          investigate the replayed search for each of them.
        </p>
        <i>Investigation of {completed}/{total} events completed.</i>
        <StyledList>
          {eventIds.map(({ id: eventId, status }) => (
            <EventListItem key={`bulk-replay-search-item-${eventId}`}
                           event={events?.[eventId]?.event}
                           selected={eventId === selectedId}
                           done={status === 'DONE'}
                           removeItem={removeItem}
                           onClick={() => selectItem(eventId)}
                           markItemAsDone={markItemAsDone} />
          ))}
        </StyledList>
        <ActionsBar>
          <RemainingBulkActions events={remainingEvents} completed={total > 0 && total === completed} />
          <Button onClick={onClose}>Close</Button>
        </ActionsBar>
      </EventsListSidebar>
      <ReplayedSearchContainer>
        <ReplayedSearch total={total} completed={completed} selectedEvent={selectedEvent} />
      </ReplayedSearchContainer>
    </Container>
  );
};

export default BulkEventReplay;
