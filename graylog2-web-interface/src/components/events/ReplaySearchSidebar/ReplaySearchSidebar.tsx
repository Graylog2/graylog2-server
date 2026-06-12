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

import usePluginEntities from 'hooks/usePluginEntities';
import GeneralEventSideBar from 'components/events/ReplaySearchSidebar/GeneralEventSideBar';

type Props = {
  alertId: string;
  definitionId?: string;
};

const ReplaySearchSidebar = ({ alertId, definitionId = undefined }: Props) => {
  const sideBarDetailsPlugin = usePluginEntities('views.components.eventReplay.sideBarDetails');
  const isEventDefinition = !alertId && !!definitionId;
  const sideBarDetails = sideBarDetailsPlugin[0];
  const hasPlugin = typeof sideBarDetails?.useCondition === 'function' ? sideBarDetails.useCondition() : true;

  const EventDefSideBar =
    hasPlugin && sideBarDetails?.eventDefinitionComponent ? sideBarDetails.eventDefinitionComponent : null;

  const EventSideBarDetails = hasPlugin && !!sideBarDetails?.component ? sideBarDetails.component : GeneralEventSideBar;

  return isEventDefinition && EventDefSideBar ? (
    <EventDefSideBar definitionId={definitionId} />
  ) : (
    <EventSideBarDetails alertId={alertId} definitionId={definitionId} />
  );
};

export default ReplaySearchSidebar;
