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

export type EventReplayInfo = {
  timerange_start: string,
  timerange_end: string,
  query: string,
  streams: string[],
  stream_categories?: string[],
};

export type Event = {
  id: string,
  event_definition_id: string,
  event_definition_type: string,
  priority: number,
  timestamp: string,
  timerange_start: string,
  timerange_end: string,
  key: string,
  fields: Record<string, string>,
  group_by_fields: { [key: string]: string },
  source_streams: string[],
  replay_info: EventReplayInfo | undefined,
  alert: boolean | undefined,
};

export type EventDefinitionContext = {
  id: string,
  title: string,
  remediation_steps?: string,
  description?: string,
};

export type EventDefinitionContexts = { [eventDefinitionId: string]: EventDefinitionContext };
export type EventsAdditionalData = {
  context: { event_definitions?: EventDefinitionContexts, streams?: EventDefinitionContexts },
}
