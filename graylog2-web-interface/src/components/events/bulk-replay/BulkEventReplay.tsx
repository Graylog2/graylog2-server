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

import useSelectedEvents from 'components/events/bulk-replay/useSelectedEvents';
import Center from 'components/common/Center';
import EventReplaySelectedProvider from 'contexts/EventReplaySelectedProvider';
import { Alert, Button } from 'components/bootstrap';
import type { SidebarSection } from 'views/components/sidebar/sidebarSections';
import sidebarSections from 'views/components/sidebar/sidebarSections';
import ReplaySearchSidebar from 'components/events/ReplaySearchSidebar/ReplaySearchSidebar';
import SidebarBulkEventReplay from 'components/events/bulk-replay/SidebarBulkEventReplay';
import EventReplaySearch from 'components/events/EventReplaySearch';
import useEventDefinition from 'hooks/useEventDefinition';
import type { SelectedEventsData } from 'contexts/EventReplaySelectedContext';

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
  events: SelectedEventsData;
  onReturnClick: () => void;
};

const AlertWrapper = ({ children = null }: React.PropsWithChildren) => (
  <Alert bsStyle="info">
    <Center>{children}</Center>
  </Alert>
);

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
    title: SidebarBulkEventReplay,
    sections: [replaySection, ...sidebarSections],
    contentColumnWidth: 350,
  },
};

const ReplayedSearch = ({
  onReturnClick,
}: React.PropsWithChildren<{
  onReturnClick: () => void;
}>) => {
  const { eventIds, selectedId, eventsData } = useSelectedEvents();
  const selectedEvent = eventsData?.[selectedId];
  const { data: eventDefinitionMappedData } = useEventDefinition(selectedEvent?.event?.event_definition_id);
  const total = eventIds.length;

  if (total === 0) {
    return (
      <AlertWrapper>
        You have removed all events from the list. You can now return back by clicking the{' '}
        <Button bsStyle="link" onClick={onReturnClick}>
          Back to events
        </Button>{' '}
        button.
      </AlertWrapper>
    );
  }

  return (
    <EventReplaySearch
      eventData={selectedEvent.event}
      eventDefinitionMappedData={eventDefinitionMappedData}
      searchPageLayout={searchPageLayout}
      forceSidebarPinned
    />
  );
};

const BulkEventReplay = ({ initialEventIds, onReturnClick, events }: Props) => (
  <EventReplaySelectedProvider initialEventIds={initialEventIds} eventsData={events}>
    <Container>
      <ReplayedSearchContainer>
        <ReplayedSearch onReturnClick={onReturnClick} />
      </ReplayedSearchContainer>
    </Container>
  </EventReplaySelectedProvider>
);

export default BulkEventReplay;
