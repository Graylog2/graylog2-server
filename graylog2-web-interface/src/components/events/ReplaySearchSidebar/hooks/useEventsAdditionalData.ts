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

import useEventDefinition from 'hooks/useEventDefinition';
import type { Event, EventsAdditionalData } from 'components/events/events/types';

const useEventsAdditionalData = ({ eventData, definitionId = null }: { eventData: Event; definitionId?: string }) => {
  const { data, isLoading: isLoadingEventDefinition } = useEventDefinition(
    definitionId ?? eventData?.event_definition_id,
  );

  const meta = useMemo<EventsAdditionalData>(
    () =>
      eventData
        ? {
            context: {
              event_definitions: {
                [eventData.event_definition_id]: data.eventDefinition,
              },
            },
          }
        : null,
    [eventData, data.eventDefinition],
  );

  const eventDefinitionEventProcedureId: string = data?.eventDefinition?.event_procedure ?? '';

  return {
    meta,
    isLoadingEventDefinition,
    eventDefinitionEventProcedureId,
  };
};

export default useEventsAdditionalData;
