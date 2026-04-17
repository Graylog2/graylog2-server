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
import Routes from 'routing/Routes';

/**
 * Builds a Graylog search URL filtered on a collector message field
 * (e.g. `collector_instance_uid`, `collector_fleet_id`, `collector_source_id`).
 * Uses a 1h relative time range and no stream scoping — the Collector System
 * Logs stream is system-scoped and is not included in unscoped searches.
 */
const collectorReceivedMessagesUrl = (field: string, value: string): string =>
  Routes.search_with_query(`${field}:"${value}"`, 'relative', { relative: 3600 });

export default collectorReceivedMessagesUrl;
