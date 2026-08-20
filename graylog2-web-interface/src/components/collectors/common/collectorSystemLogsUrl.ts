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

import { COLLECTOR_INSTANCE_UID_FIELD, COLLECTOR_SYSTEM_LOGS_STREAM_ID } from './fields';

const collectorSystemLogsUrl = (instanceUid: string): string =>
  Routes.search_with_query(`${COLLECTOR_INSTANCE_UID_FIELD}:"${instanceUid}"`, 'relative', { relative: 3600 }, [
    COLLECTOR_SYSTEM_LOGS_STREAM_ID,
  ]);

export default collectorSystemLogsUrl;
