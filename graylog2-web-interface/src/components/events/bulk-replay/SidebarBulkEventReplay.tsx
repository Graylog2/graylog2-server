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
import { useState, useCallback, useMemo } from 'react';
import styled, { css } from 'styled-components';

import Store from 'logic/local-storage/Store';
import EventListItem from 'components/events/bulk-replay/EventListItem';
import useSelectedEvents from 'components/events/bulk-replay/useSelectedEvents';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import type { RemainingBulkActionsProps } from 'components/events/bulk-replay/types';
import { Icon, IconButton } from 'components/common';
import useEventBulkActions from 'components/events/events/hooks/useEventBulkActions';
import Popover from 'components/common/Popover';
import { REPLAY_SESSION_ID_PARAM } from 'components/events/Constants';
import useRoutingQuery from 'routing/useQuery';
import { Button } from 'components/bootstrap';
import useEventsById from 'components/events/bulk-replay/hooks/useEventsById';
import DropdownButton from 'components/bootstrap/DropdownButton';

const Container = styled.div`
  display: flex;
`;

const EventsListSidebar = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
    width: 300px;
  `,
);

const StyledList = styled.ul`
  padding-inline-start: 0;
  max-height: 400px;
  overflow-y: auto;
`;

const ActionsBar = styled(ButtonToolbar)`
  align-self: flex-end;
  display: flex;
  justify-content: flex-end;
  align-items: end;
  gap: 0.25em;
`;

type Props = {
  onClose: () => void;
};

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

const CurrentContainer = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-right: ${theme.spacings.md};
  `,
);

const ArrowButton = styled(Button)`
  padding: 0;
  border: 0;
`;

const TargetContainer = styled.div`
  flex-grow: 1;
`;

const CollapseButton = styled(IconButton)(
  () => css`
    position: absolute;
    right: 0;
    align-self: flex-start;
  `,
);
const SidebarBulkEventReplay = ({ onClose }: Props) => {
  const params = useRoutingQuery();
  const replaySessionId = params[REPLAY_SESSION_ID_PARAM];
  const initialEventIds: Array<string> = Store.sessionGet(replaySessionId);
  const { data: _events } = useEventsById(initialEventIds);

  const [events] = useState(_events);
  const { eventIds, selectedId, removeItem, selectItem, markItemAsDone } = useSelectedEvents();
  const total = eventIds.length;
  const completed = eventIds.filter((event) => event.status === 'DONE').length;
  const remainingEvents = eventIds.map((eventId) => events[eventId.id]?.event);
  const curIndex = useMemo(() => eventIds.findIndex((item) => item.id === selectedId), [eventIds, selectedId]);

  const onGoBack = useCallback(() => {
    selectItem(eventIds[curIndex - 1].id);
  }, [curIndex, eventIds, selectItem]);
  const onGoForward = useCallback(() => {
    selectItem(eventIds[curIndex + 1].id);
  }, [curIndex, eventIds, selectItem]);

  if (!initialEventIds?.length) return null;

  const status = eventIds?.find((state) => state.id === selectedId)?.status;

  return (
    <CurrentContainer>
      <ArrowButton onClick={onGoBack} disabled={curIndex === 0}>
        <Icon name="keyboard_arrow_left" title="Previous Event" />
      </ArrowButton>
      <Popover position="bottom">
        <Popover.Target>
          <TargetContainer>
            <EventListItem
              key={`bulk-replay-search-item-${selectedId}`}
              event={events?.[selectedId]?.event}
              selected={false}
              done={status === 'DONE'}
              removeItem={removeItem}
              onClick={() => {}}
              markItemAsDone={markItemAsDone}
              isDropdown
            />
          </TargetContainer>
        </Popover.Target>
        <Popover.Dropdown title="Info">
          <Container>
            <EventsListSidebar>
              <b>Selected events</b>
              <p>
                The following list contains all of the events/alerts you selected in the previous step, allowing you to
                review the replayed search for each of them.
              </p>
              <i>
                Review of {completed}/{total} events completed.
              </i>
              <StyledList>
                {eventIds.map(({ id: eventId, status: eventStatus }) => (
                  <EventListItem
                    key={`bulk-replay-search-item-${eventId}`}
                    event={events?.[eventId]?.event}
                    selected={eventId === selectedId}
                    done={eventStatus === 'DONE'}
                    removeItem={removeItem}
                    onClick={() => selectItem(eventId)}
                    markItemAsDone={markItemAsDone}
                  />
                ))}
              </StyledList>
              <ActionsBar>
                <RemainingBulkActions events={remainingEvents} completed={total > 0 && total === completed} />
              </ActionsBar>
            </EventsListSidebar>
          </Container>
        </Popover.Dropdown>
      </Popover>
      <ArrowButton onClick={onGoForward} disabled={curIndex === eventIds.length - 1}>
        <Icon name="keyboard_arrow_right" title="Next Event" />
      </ArrowButton>
      <CollapseButton name="keyboard_tab_rtl" title="Collapse sidebar" onClick={onClose} />
    </CurrentContainer>
  );
};

export default SidebarBulkEventReplay;
