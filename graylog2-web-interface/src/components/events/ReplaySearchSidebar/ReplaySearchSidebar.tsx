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

import usePluginEntities from 'hooks/usePluginEntities';
import { useAssistantPanelContext } from 'components/assistant';
import GeneralEventSideBar from 'components/events/ReplaySearchSidebar/GeneralEventSideBar';

type Props = {
  alertId: string;
  definitionId?: string;
};

const ReplaySearchSidebar = ({ alertId, definitionId = undefined }: Props) => {
  const sideBarDetails = usePluginEntities('views.components.eventReplay.sideBarDetails')[0];
  // The plugin entity is fixed for the lifetime of the page, so this hook is always called in the same order.
  const hasPlugin = sideBarDetails?.useCondition?.() ?? true;
  const isEventDefinition = !alertId && !!definitionId;
  useAssistantPanelContext(
    useMemo(() => (alertId ? { type: 'event' as const, event_id: alertId } : undefined), [alertId]),
  );

  const EventDefSideBar = hasPlugin ? sideBarDetails?.eventDefinitionComponent : undefined;
  const EventSideBarDetails = (hasPlugin ? sideBarDetails?.component : undefined) ?? GeneralEventSideBar;

  if (isEventDefinition && EventDefSideBar) {
    return <EventDefSideBar definitionId={definitionId} />;
  }

  return <EventSideBarDetails alertId={alertId} definitionId={definitionId} />;
};

export default ReplaySearchSidebar;
