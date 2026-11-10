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

/**
 * Message field names and well-known values written by the collector ingest path.
 * These mirror the backend definitions in `CollectorIngestCodec` and
 * `CollectorLogRecordProcessor` — keep them in sync.
 */
export const AGENT_ID_FIELD = 'agent_id';
export const AGENT_FLEET_ID_FIELD = 'agent_fleet_id';
export const AGENT_SOURCE_ID_FIELD = 'agent_source_id';
export const AGENT_RECEIVER_TYPE_FIELD = 'agent_receiver_type';

/** `agent_receiver_type` value identifying collector self-logs (supervisor + OTel process). */
export const COLLECTOR_LOG_RECEIVER_TYPE = 'collector_log';

/** Built-in stream collector self-logs are routed to (`Stream.COLLECTOR_SYSTEM_LOGS_STREAM_ID`). */
export const COLLECTOR_SYSTEM_LOGS_STREAM_ID = '000000000000000000000005';
