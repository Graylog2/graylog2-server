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

import type { TimeRange } from 'views/logic/queries/Query';

export type SearchesConfig = {
  surrounding_timerange_options: { [key: string]: string },
  surrounding_filter_fields: Array<string>,
  query_time_range_limit: string,
  relative_timerange_options: { [key: string]: string },
  analysis_disabled_fields: Array<string>,
  auto_refresh_timerange_options: { [key: string]: string },
  default_auto_refresh_option: string,
  quick_access_timerange_presets: Array<{ description: string, timerange: TimeRange, id: string}>,
  cancel_after_seconds: number,
};
