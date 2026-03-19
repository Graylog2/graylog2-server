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
import useFeature from 'hooks/useFeature';
import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';
import sidebarSections, { type SidebarSection } from 'views/components/sidebar/sidebarSections';
import useReplaySearchContext from 'components/event-definitions/replay-search/hooks/useReplaySearchContext';
import SidebarEventDetails from 'components/events/SidebarEventDetails';

const ReplaySearchSidebarSection = () => {
  const { alertId, definitionId } = useReplaySearchContext();

  return <ReplaySearchSidebar alertId={alertId} definitionId={definitionId} />;
};

const replaySection: SidebarSection = {
  key: 'eventDescription',
  hoverTitle: 'Replay Details',
  title: null,
  icon: 'play_arrow',
  content: ReplaySearchSidebarSection,
};

const defaultSearchPageLayout: Partial<LayoutState> = {
  sidebar: {
    isShown: true,
    title: 'Replayed Search',
    sections: [...sidebarSections],
    contentColumnWidth: 350,
  },
};

const legacySearchPageLayout: Partial<LayoutState> = {
  sidebar: {
    isShown: true,
    title: 'Replayed Search',
    sections: [replaySection, ...sidebarSections],
    contentColumnWidth: 350,
  },
};

type Props = {
  eventDefinitionMappedData: EventDefinitionMappedData;
  eventData: Event;
  searchPageLayout?: Partial<LayoutState>;
};

const EventReplaySearch = ({ eventDefinitionMappedData, eventData, searchPageLayout = undefined }: Props) => {
  const { eventDefinition, aggregations } = eventDefinitionMappedData;
  const { openSidebar } = useRightSidebar();
  const isRightSidebarEnabled = useFeature('replay_search_right_sidebar');
  const effectiveLayout =
    searchPageLayout ?? (isRightSidebarEnabled ? defaultSearchPageLayout : legacySearchPageLayout);

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
    if (isRightSidebarEnabled) {
      openSidebar(SidebarEventDetails(eventData?.id, eventDefinition?.id));
    }
  }, [isRightSidebarEnabled, openSidebar, eventData?.id, eventDefinition?.id]);

  return (
    <ReplaySearchContext.Provider value={replaySearchContext}>
      <ReplaySearch view={view} searchPageLayout={effectiveLayout} />
    </ReplaySearchContext.Provider>
  );
};

const WithLoadingBarrier = ({ eventDefinitionMappedData, eventData, searchPageLayout = undefined }: Props) => (
  <LoadingBarrier eventDefinition={eventDefinitionMappedData.eventDefinition}>
    <EventReplaySearch
      eventDefinitionMappedData={eventDefinitionMappedData}
      eventData={eventData}
      searchPageLayout={searchPageLayout}
    />
  </LoadingBarrier>
);

export default WithLoadingBarrier;
