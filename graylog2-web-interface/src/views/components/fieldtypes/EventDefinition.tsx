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
import { create, keyResolver, windowScheduler } from "@yornaath/batshit";
import { useQuery } from '@tanstack/react-query';

import { EventsDefinitions} from '@graylog/server-api';

import Spinner from 'components/common/Spinner';

const fetchEventDefinition = create({
  fetcher: async (ids: Array<string>) => EventsDefinitions.getById({ event_definition_ids: ids }),
  resolver: keyResolver("id"),
  scheduler: windowScheduler(10),
});

const EventDefinition = ({ value }: { value: string }) => {
  const { data: eventDefinition, isLoading } = useQuery(['event-definitions', 'batched', value], () => fetchEventDefinition.fetch(value));

  if (isLoading) {
    return <Spinner />;
  }

  return eventDefinition?.title
    ? <span>{eventDefinition.title}</span>
    : <i>Missing Event Definition: {value}</i>;
};

export default EventDefinition;
