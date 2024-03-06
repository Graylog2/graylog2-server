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

/* eslint-disable no-template-curly-in-string */
export const DEFAULT_JSON_TEMPLATE: string = '{\n'
  + '  "event_definition_id": "${event_definition_id}",\n'
  + '  "event_definition_type": "${event_definition_type}",\n'
  + '  "event_title": "${event_definition_title}",\n'
  + '  "event_definition_description": "${event_definition_description}",\n'
  + '  "job_definition_id": "${job_definition_id}",\n'
  + '  "event_id": "${event.id}",\n'
  + '  "event_origin_context": "${event.origin_context}",\n'
  + '  "event_timestamp_processing": "${event.timestamp_processing}",\n'
  + '  "event_timerange_start": "${event.timerange_start}",\n'
  + '  "event_timerange_end": "${event.timerange_end}",\n'
  + '  "event_streams": "${event.streams}",\n'
  + '  "event_source_streams": "${event.source_streams}",\n'
  + '  "event_alert": "${event.alert}",\n'
  + '  "event_message": "${event.message}",\n'
  + '  "event_source": "${event.source}",\n'
  + '  "event_key": "${event.key}",\n'
  + '  "event_priority": "${event.priority}"${if backlog},\n'
  + '  "backlog": [\n'
  + '  ${foreach backlog message}\n'
  + '  { "id": "${message.id}",\n'
  + '    "message": "${message.message}"}${if ! last_message},${end}${end}\n'
  + '  ]${end}\n'
  + '}';

export const DEFAULT_FORM_PARAM_TEMPLATE: string = 'event_definition_id=${event_definition_id}&event_definition_type=${event_definition_type}'
  + '&event_title=${event_definition_title}&event_definition_description=${event_definition_description}&job_definition_id=${job_definition_id}'
  + '&event_id=${event.id}&event_origin_context=${event.origin_context}&event_timestamp_processing=${event.timestamp_processing}'
  + '&event_timerange_start=${event.timerange_start}&event_timerange_end=${event.timerange_end}&event_streams=${event.streams}'
  + '&event_source_streams=${event.source_streams}&event_alert=${event.alert}&event_message=${event.message}&event_source=${event.source}'
  + '&event_key=${event.key}&event_priority=${event.priority}';

export const DEFAULT_PLAIN_TEXT_TEMPLATE: string = 'event_definition_id=${event_definition_id}\n'
  + 'event_definition_type=${event_definition_type}\n'
  + 'event_title=${event_definition_title}\n'
  + 'event_definition_description=${event_definition_description}\n'
  + 'job_definition_id=${job_definition_id}\n'
  + 'event_id=${event.id}\n'
  + 'event_origin_context=${event.origin_context}\n'
  + 'event_timestamp_processing=${event.timestamp_processing}\n'
  + 'event_timerange_start=${event.timerange_start}\n'
  + 'event_timerange_end=${event.timerange_end}\n'
  + 'event_streams=${event.streams}\n'
  + 'event_source_streams=${event.source_streams}\n'
  + 'event_alert=${event.alert}\n'
  + 'event_message=${event.message}\n'
  + 'event_source=${event.source}\n'
  + 'event_key=${event.key}\n'
  + 'event_priority=${event.priority}\n'
  + '${if backlog}backlog={backlog}${end}';
