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
import { create, keyResolver, windowScheduler } from '@yornaath/batshit';
import { useQuery } from '@tanstack/react-query';

import { EventsDefinitions } from '@graylog/server-api';

// Lookups made within 10ms of each other, e.g. by every row of a table, go out as one request.
const eventDefinitionsBatcher = create({
  fetcher: async (ids: Array<string>) => EventsDefinitions.getById({ event_definition_ids: ids }),
  resolver: keyResolver('id'),
  scheduler: windowScheduler(10),
});

// Resolves to null for definitions that are missing or that the user can't read.
const useEventDefinitionBatched = (eventDefinitionId: string | undefined) =>
  useQuery({
    queryKey: ['event-definitions', 'batched', eventDefinitionId],
    queryFn: () => eventDefinitionsBatcher.fetch(eventDefinitionId).then((eventDefinition) => eventDefinition ?? null),
    enabled: !!eventDefinitionId,
  });

export default useEventDefinitionBatched;
