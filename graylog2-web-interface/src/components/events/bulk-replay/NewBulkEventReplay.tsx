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
// @ts-nocheck
import * as React from 'react';
import { useState } from 'react';
import styled, { css } from 'styled-components';
import { useQuery } from '@tanstack/react-query';

import { Events } from '@graylog/server-api';

import EventListItem from 'components/events/bulk-replay/EventListItem';
import useSelectedEvents from 'components/events/bulk-replay/useSelectedEvents';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import type { RemainingBulkActionsProps } from 'components/events/bulk-replay/types';
import { HoverForHelp } from 'components/common';
import useLocation from 'routing/useLocation';
import type { BulkEventReplayState } from 'views/pages/BulkEventReplayPage';
import useEventBulkActions from 'components/events/events/hooks/useEventBulkActions';

import DropdownButton from '../../bootstrap/DropdownButton';

const Container = styled.div`
  display: flex;
`;

const EventsListSidebar = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
    width: 300px;
    // max-width: 500px;
  `,
);

const StyledList = styled.ul`
  padding-inline-start: 0;
`;

const ActionsBar = styled(ButtonToolbar)`
  align-self: flex-end;
  display: flex;
  justify-content: flex-end;
  align-items: end;
  gap: 0.25em;
`;

type Props = {
  BulkActions?: React.ComponentType<RemainingBulkActionsProps>;
};

const useEventsById = (eventIds: Array<string>) =>
  useQuery({
    queryKey: ['events', eventIds],
    queryFn: () => Events.getByIds({ event_ids: eventIds }),
  });

const RemainingBulkActions = ({ completed, events }: RemainingBulkActionsProps) => {
  const { actions, pluggableActionModals } = useEventBulkActions(events);

  return (
    <>
      <DropdownButton
        title="Bulk actions"
        bsStyle={completed ? 'success' : 'default'}
        id="bulk-actions-dropdown"
        disabled={!events?.length}
        bsSize="xs">
        {actions}
      </DropdownButton>
      {pluggableActionModals}
    </>
  );
};

const InfoBarBulkEventReplay = ({ BulkActions = RemainingBulkActions }: Props) => {
  const location = useLocation<BulkEventReplayState>();
  const { eventIds: initialEventIds = [] } = location?.state ?? {};
  const { data: _events } = useEventsById(initialEventIds);

  const [events] = useState(_events);
  const { eventIds, selectedId, removeItem, selectItem, markItemAsDone } = useSelectedEvents(initialEventIds);
  const total = eventIds.length;
  const completed = eventIds.filter((event) => event.status === 'DONE').length;
  const remainingEvents = eventIds.map((eventId) => events[eventId.id]?.event);

  if (!initialEventIds?.length) return null;

  return (
    <Container>
      <EventsListSidebar>
        <b>
          Selected events{' '}
          <HoverForHelp>
            <p>
              The following list contains all of the events/alerts you selected in the previous step, allowing you to
              review the replayed search for each of them.
            </p>
          </HoverForHelp>
        </b>
        <i>
          Review of {completed}/{total} events completed.
        </i>
        <StyledList>
          {eventIds.map(({ id: eventId, status }) => (
            <EventListItem
              key={`bulk-replay-search-item-${eventId}`}
              event={events?.[eventId]?.event}
              selected={eventId === selectedId}
              done={status === 'DONE'}
              removeItem={removeItem}
              onClick={() => selectItem(eventId)}
              markItemAsDone={markItemAsDone}
            />
          ))}
        </StyledList>
        <ActionsBar>
          <BulkActions events={remainingEvents} completed={total > 0 && total === completed} />
        </ActionsBar>
      </EventsListSidebar>
    </Container>
  );
};

export default InfoBarBulkEventReplay;
