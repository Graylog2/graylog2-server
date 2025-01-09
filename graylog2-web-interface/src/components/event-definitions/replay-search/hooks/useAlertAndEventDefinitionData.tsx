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

import type { Event } from 'components/events/events/types';
import type { EventDefinitionAggregation } from 'hooks/useEventDefinition';
import useEventDefinition from 'hooks/useEventDefinition';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import useEventById from 'hooks/useEventById';

const useAlertAndEventDefinitionData = (alertId: string, definitionId?: string) => {
  const { data: eventData, isLoading: isLoadingEvent } = useEventById(alertId);
  const { data, isLoading: isLoadingEventDefinition } = useEventDefinition(definitionId ?? eventData?.event_definition_id);
  const eventDefinition = data?.eventDefinition;
  const aggregations = data?.aggregations;
  const isLoading = (alertId && isLoadingEvent) || (definitionId && isLoadingEventDefinition);

  return useMemo<{
    alertId: string,
    definitionId: string,
    definitionTitle: string,
    eventData: Event,
    eventDefinition: EventDefinition,
    aggregations: Array<EventDefinitionAggregation>,
    isLoading: boolean,
  }>(() => ({
    alertId,
    definitionId: eventDefinition?.id,
    definitionTitle: eventDefinition?.title,
    eventData,
    eventDefinition,
    aggregations,
    isLoading,
  }), [alertId, eventDefinition, eventData, aggregations, isLoading]);
};

export default useAlertAndEventDefinitionData;
