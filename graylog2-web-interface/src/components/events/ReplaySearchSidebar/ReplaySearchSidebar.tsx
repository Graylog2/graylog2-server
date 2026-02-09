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

import useReplaySearchContext from 'components/event-definitions/replay-search/hooks/useReplaySearchContext';
import usePluginEntities from 'hooks/usePluginEntities';
import GeneralEventSideBar from 'components/events/ReplaySearchSidebar/GeneralEventSideBar';
import type { EventReplaySideBarDetailsProps } from 'views/types';

const ReplaySearchSidebar = () => {
  const { alertId } = useReplaySearchContext();

  const sideBarDetailsPlugin = usePluginEntities('views.components.eventReplay.sideBarDetails');

  const EventSideBarDetails = useMemo<React.ComponentType<EventReplaySideBarDetailsProps>>(() => {
    const sideBarDetails = sideBarDetailsPlugin[0];

    const hasPlugin = typeof sideBarDetails?.useCondition === 'function' ? sideBarDetails.useCondition() : true;

    if (hasPlugin && !!sideBarDetails?.component) return sideBarDetails.component;

    return GeneralEventSideBar;
  }, [sideBarDetailsPlugin]);

  return <EventSideBarDetails alertId={alertId} />;
};

export default ReplaySearchSidebar;
