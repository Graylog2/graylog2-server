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
import useCreateSearch from 'views/hooks/useCreateSearch';
import useRightSidebar from 'hooks/useRightSidebar';
import ReplaySearchSidebar from 'components/events/ReplaySearchSidebar/ReplaySearchSidebar';
import useFeature from 'hooks/useFeature';
import sidebarSections, { type SidebarSection } from 'views/components/sidebar/sidebarSections';
import EventDefinitionSideBar from 'components/event-definitions/replay-search/EventDefinitionSideBar';
import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';

type Props = {
  eventDefinitionMappedData: EventDefinitionMappedData;
};

const replaySection: SidebarSection = {
  key: 'eventDescription',
  hoverTitle: 'Alert/Event Details',
  title: null,
  icon: 'play_arrow',
  content: EventDefinitionSideBar,
};

const legacySearchPageLayout: Partial<LayoutState> = {
  sidebar: {
    isShown: true,
    title: 'Replayed Search',
    sections: [replaySection, ...sidebarSections],
    contentColumnWidth: 300,
  },
};

const EventDefinitionReplaySearch = ({ eventDefinitionMappedData }: Props) => {
  const { eventDefinition, aggregations } = eventDefinitionMappedData;
  const { openSidebar } = useRightSidebar();
  const isRightSidebarEnabled = useFeature('replay_search_right_sidebar');

  const _view = useCreateViewForEvent({
    eventData: undefined,
    eventDefinition,
    aggregations,
  });
  const view = useCreateSearch(_view);
  const replaySearchContext = useMemo<ReplaySearchContextType>(
    () => ({
      alertId: null,
      definitionId: eventDefinition.id,
      type: 'event_definition',
    }),
    [eventDefinition.id],
  );

  useEffect(() => {
    if (isRightSidebarEnabled) {
      openSidebar({
        id: 'replay-search-sidebar',
        title: 'Event Definition Details',
        component: ReplaySearchSidebar,
        props: { alertId: undefined, definitionId: eventDefinition.id },
      });
    }
  }, [isRightSidebarEnabled, openSidebar, eventDefinition.id]);

  return (
    <ReplaySearchContext.Provider value={replaySearchContext}>
      <ReplaySearch view={view} searchPageLayout={isRightSidebarEnabled ? undefined : legacySearchPageLayout} />
    </ReplaySearchContext.Provider>
  );
};

const WithLoadingBarrier = ({ eventDefinitionMappedData }: Props) => (
  <LoadingBarrier eventDefinition={eventDefinitionMappedData.eventDefinition}>
    <EventDefinitionReplaySearch eventDefinitionMappedData={eventDefinitionMappedData} />
  </LoadingBarrier>
);

export default WithLoadingBarrier;
