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

import { useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import useLocation from 'routing/useLocation';
import Routes from 'routing/Routes';
import useParams from 'routing/useParams';
import type { Event } from 'components/events/events/types';
import type { EventDefinitionAggregation } from 'hooks/useEventDefinition';
import type { EventDefinition } from 'logic/alerts/types';

const useAlertAndEventDefinitionData = () => {
  const { pathname: path } = useLocation();
  const { alertId, definitionId } = useParams<{ alertId?: string, definitionId?: string }>();
  const queryClient = useQueryClient();
  const eventData = queryClient.getQueryData(['event-by-id', alertId]) as Event;
  const data = queryClient.getQueryData(['event-definition-by-id', definitionId || eventData?.event_definition_id]) as { eventDefinition: EventDefinition, aggregations: Array<EventDefinitionAggregation>};
  const eventDefinition = data?.eventDefinition;
  const aggregations = data?.aggregations;

  return useMemo<{
    alertId: string,
    definitionId: string,
    definitionTitle: string,
    isAlert: boolean,
    isEvent: boolean,
    isEventDefinition: boolean,
    eventData: Event,
    eventDefinition: EventDefinition,
    aggregations: Array<EventDefinitionAggregation>,
  }>(() => ({
    alertId,
    definitionId: eventDefinition?.id,
    definitionTitle: eventDefinition?.title,
    isAlert: (path === Routes.ALERTS.replay_search(alertId)) && eventData && eventData?.alert,
    isEvent: !!alertId && (path === Routes.ALERTS.replay_search(alertId)) && eventData && !eventData?.alert,
    isEventDefinition: !!definitionId && (path === Routes.ALERTS.DEFINITIONS.replay_search(definitionId)) && !!eventDefinition,
    eventData,
    eventDefinition,
    aggregations,
  }), [alertId, eventDefinition, path, eventData, definitionId, aggregations]);
};

export default useAlertAndEventDefinitionData;
