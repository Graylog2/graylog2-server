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
import React, { useMemo } from 'react';

import useCreateViewForEvent from 'views/logic/views/UseCreateViewForEvent';
import type { EventDefinitionMappedData } from 'hooks/useEventDefinition';
import ReplaySearch, { LoadingBarrier } from 'components/events/ReplaySearch';
import type { ReplaySearchContextType } from 'components/event-definitions/replay-search/ReplaySearchContext';
import ReplaySearchContext from 'components/event-definitions/replay-search/ReplaySearchContext';
import type { Event } from 'components/events/events/types';
import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';
import ReplaySearchSidebar from 'components/events/ReplaySearchSidebar/ReplaySearchSidebar';
import sidebarSections, { type SidebarSection } from 'views/components/sidebar/sidebarSections';

type Props = {
  eventDefinitionMappedData: EventDefinitionMappedData;
  eventData: Event;
  searchPageLayout?: Partial<LayoutState>;
};

const replaySection: SidebarSection = {
  key: 'eventDescription',
  hoverTitle: 'Replay Details',
  title: null,
  icon: 'play_arrow',
  content: ReplaySearchSidebar,
};

const defaultSearchPageLayout: Partial<LayoutState> = {
  sidebar: {
    isShown: true,
    title: 'Replayed Search',
    sections: [replaySection, ...sidebarSections],
    contentColumnWidth: 350,
  },
};

const EventReplaySearch = ({
  eventDefinitionMappedData,
  searchPageLayout = defaultSearchPageLayout,
  eventData,
}: Props) => {
  const { eventDefinition, aggregations } = eventDefinitionMappedData;

  const view = useCreateViewForEvent({
    eventData,
    eventDefinition,
    aggregations,
  });

  const replaySearchContext = useMemo<ReplaySearchContextType>(
    () => ({
      alertId: eventData?.id,
      definitionId: eventDefinition?.id,
      type: eventData?.alert ? 'alert' : 'event',
    }),
    [eventData?.alert, eventData?.id, eventDefinition?.id],
  );

  return (
    <ReplaySearchContext.Provider value={replaySearchContext}>
      <ReplaySearch view={view} searchPageLayout={searchPageLayout} />
    </ReplaySearchContext.Provider>
  );
};

const WithLoadingBarrier = ({
  eventDefinitionMappedData,
  searchPageLayout = defaultSearchPageLayout,
  eventData,
}: Props) => (
  <LoadingBarrier eventDefinition={eventDefinitionMappedData.eventDefinition}>
    <EventReplaySearch
      eventDefinitionMappedData={eventDefinitionMappedData}
      searchPageLayout={searchPageLayout}
      eventData={eventData}
    />
  </LoadingBarrier>
);

export default WithLoadingBarrier;
