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
import md5 from 'md5';

import MessageFormatter from 'logic/message/MessageFormatter';
import * as URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import { MESSAGE_FIELD, SOURCE_FIELD } from 'views/Constants';
import type { TimeRange } from 'views/logic/queries/Query';

const extractTimeRange = (type: string, timerange: TimeRange): TimeRange => {
  // The server API uses the `range` parameter instead of `relative` for indicating a relative time range.
  if (type === 'relative') {
    // @ts-ignore
    return { type: 'relative', range: timerange.range || timerange.relative };
  }

  return timerange;
};

const DEFAULT_LIMIT = 150;
const universalSearch = (
  type: TimeRange['type'],
  query: string,
  timerange: TimeRange,
  streamId: string,
  limit: number = DEFAULT_LIMIT,
  page: number = undefined,
  sortField: string = undefined,
  sortOrder: 'asc' | 'desc' = undefined,
  decorate: boolean = undefined,
) => {
  const timerangeParams = extractTimeRange(type, timerange);
  const offset = (page - 1) * limit;

  const url = URLUtils.qualifyUrl(
    ApiRoutes.UniversalSearchApiController.search(
      type,
      query,
      timerangeParams,
      streamId,
      limit,
      offset,
      sortField,
      sortOrder,
      decorate,
    ).url,
  );

  return fetch('GET', url).then((response) => ({
    ...response,
    fields: response.fields.map((field) => ({
      hash: md5(field),
      name: field,
      standard_selected: field === MESSAGE_FIELD || field === SOURCE_FIELD,
    })),
    messages: response.messages.map((message) => MessageFormatter.formatMessageSummary(message)),
  }));
};

export default universalSearch;
