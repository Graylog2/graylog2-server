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
import React, { useEffect, useMemo } from 'react';

import useCreateViewForEvent from 'views/logic/views/UseCreateViewForEvent';
import type { EventDefinitionMappedData } from 'hooks/useEventDefinition';
import ReplaySearch, { LoadingBarrier } from 'components/events/ReplaySearch';
import type { ReplaySearchContextType } from 'components/event-definitions/replay-search/ReplaySearchContext';
import ReplaySearchContext from 'components/event-definitions/replay-search/ReplaySearchContext';
import type { Event } from 'components/events/events/types';
import useRightSidebar from 'hooks/useRightSidebar';
import ReplaySearchSidebar from 'components/events/ReplaySearchSidebar/ReplaySearchSidebar';
import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';
import sidebarSections from 'views/components/sidebar/sidebarSections';

const defaultSearchPageLayout: Partial<LayoutState> = {
  sidebar: {
    isShown: true,
    title: 'Replayed Search',
    sections: [...sidebarSections],
    contentColumnWidth: 350,
  },
};

type Props = {
  eventDefinitionMappedData: EventDefinitionMappedData;
  eventData: Event;
  searchPageLayout?: Partial<LayoutState>;
};

const EventReplaySearch = ({
  eventDefinitionMappedData,
  eventData,
  searchPageLayout = defaultSearchPageLayout,
}: Props) => {
  const { eventDefinition, aggregations } = eventDefinitionMappedData;
  const { openSidebar } = useRightSidebar();

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

  useEffect(() => {
    openSidebar({
      id: 'replay-search-sidebar',
      title: 'Replay Details',
      component: ReplaySearchSidebar,
      props: { alertId: eventData?.id, definitionId: eventDefinition?.id },
    });
  }, [openSidebar, eventData?.id, eventDefinition?.id]);

  return (
    <ReplaySearchContext.Provider value={replaySearchContext}>
      <ReplaySearch view={view} searchPageLayout={searchPageLayout} />
    </ReplaySearchContext.Provider>
  );
};

const WithLoadingBarrier = ({
  eventDefinitionMappedData,
  eventData,
  searchPageLayout,
}: Props) => (
  <LoadingBarrier eventDefinition={eventDefinitionMappedData.eventDefinition}>
    <EventReplaySearch
      eventDefinitionMappedData={eventDefinitionMappedData}
      eventData={eventData}
      searchPageLayout={searchPageLayout}
    />
  </LoadingBarrier>
);

export default WithLoadingBarrier;
