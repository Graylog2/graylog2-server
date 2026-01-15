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
  timerange_start: string;
  timerange_end: string;
  query: string;
  streams: string[];
  stream_categories?: string[];
};

export interface Event {
  timerange_end: string;
  timestamp_processing?: string;
  origin_context?: string;
  scores?: {
    [_key: string]: number;
  };
  replay_info: EventReplayInfo;
  streams?: string[];
  source_streams: string[];
  source?: string;
  priority: number;
  message: string;
  associated_assets?: string[];
  group_by_fields: {
    [_key: string]: string;
  };
  key_tuple?: string[];
  alert: boolean;
  event_definition_type: string;
  event_definition_id: string;
  id: string;
  fields: {
    [_key: string]: string;
  };
  key: string;
  aggregation_conditions?: {
    [_key: string]: number;
  };
  timestamp: string;
  timerange_start: string;
}

export type EventDefinitionContext = {
  id: string;
  title: string;
  remediation_steps?: string;
  event_procedure?: string;
  description?: string;
};

export type EventDefinitionContexts = { [eventDefinitionId: string]: EventDefinitionContext };
export type EventsAdditionalData = {
  context: { event_definitions?: EventDefinitionContexts; streams?: EventDefinitionContexts };
};
