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
import styled, { css } from 'styled-components';

import EventListItem from 'components/events/bulk-replay/EventListItem';
import useSelectedEvents from 'components/events/bulk-replay/useSelectedEvents';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import type { RemainingBulkActionsProps } from 'components/events/bulk-replay/types';
import useEventBulkActions from 'components/events/events/hooks/useEventBulkActions';
import { Alert } from 'components/bootstrap';
import DropdownButton from 'components/bootstrap/DropdownButton';
import useSessionInitialEventIds from 'components/events/bulk-replay/hooks/useSessionInitialEventIds';

const Container = styled.div`
  display: flex;
`;

const Row = styled.div`
  display: flex;
  justify-content: space-between;
`;

const EventsListSidebar = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
  `,
);

const StyledList = styled.ul(
  ({ theme }) => css`
    padding-inline-start: 0;
    max-height: 400px;
    overflow-y: auto;
    padding-top: ${theme.spacings.xs};
  `,
);

const ActionsBar = styled(ButtonToolbar)`
  align-self: flex-start;
  display: flex;
  justify-content: flex-end;
  align-items: end;
  gap: 0.25em;
`;

const RemainingBulkActions = ({ completed, actions }: RemainingBulkActionsProps) => (
  <DropdownButton
    title="Bulk actions"
    bsStyle={completed ? 'success' : 'default'}
    id="bulk-actions-dropdown"
    bsSize="xs">
    {actions}
  </DropdownButton>
);

const SidebarBulkEventReplay = () => {
  const initialEventIds: Array<string> = useSessionInitialEventIds();

  const { eventIds, eventsData: events, selectedId, removeItem, selectItem, markItemAsDone } = useSelectedEvents();
  const total = eventIds.length;
  const completed = eventIds.filter((event) => event.status === 'DONE').length;
  const remainingEvents = eventIds.map((eventId) => events[eventId.id]?.event);
  const { actions, pluggableActionModals } = useEventBulkActions(remainingEvents);

  if (!initialEventIds?.length) return null;

  return (
    <Container>
      <EventsListSidebar>
        <p>
          The following list contains all of the events/alerts you selected in the previous step, allowing you to review
          the replayed search for each of them.
        </p>
        <Row>
          <i>
            Review of {completed}/{total} events completed.
          </i>
          {actions && (
            <ActionsBar>
              <RemainingBulkActions actions={actions} completed={total > 0 && total === completed} />
            </ActionsBar>
          )}
        </Row>
        {total === completed && (
          <Alert bsStyle="info">
            You have reviewed all events. You can now select a bulk action to apply to all events listed below.
          </Alert>
        )}
        <StyledList>
          {eventIds.map(({ id: eventId, status: eventStatus }) => (
            <li key={`bulk-replay-search-item-${eventId}`}>
              <EventListItem
                event={events?.[eventId]?.event}
                selected={eventId === selectedId}
                done={eventStatus === 'DONE'}
                removeItem={removeItem}
                onClick={() => selectItem(eventId)}
                markItemAsDone={markItemAsDone}
              />
            </li>
          ))}
        </StyledList>
      </EventsListSidebar>
      {pluggableActionModals}
    </Container>
  );
};

export default SidebarBulkEventReplay;
