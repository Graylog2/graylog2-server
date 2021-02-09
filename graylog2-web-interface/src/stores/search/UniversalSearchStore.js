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
import Reflux from 'reflux';
import jQuery from 'jquery';
import md5 from 'md5';

import MessageFormatter from 'logic/message/MessageFormatter';
import * as URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

const UniversalSearchStore = Reflux.createStore({
  DEFAULT_LIMIT: 150,
  listenables: [],

  search(type, query, timerange, streamId, limit, page, sortField, sortOrder, decorate) {
    const timerangeParams = UniversalSearchStore.extractTimeRange(type, timerange);
    const effectiveLimit = limit || this.DEFAULT_LIMIT;
    const offset = (page - 1) * effectiveLimit;

    const url = URLUtils.qualifyUrl(ApiRoutes.UniversalSearchApiController.search(type, query,
      timerangeParams, streamId, effectiveLimit, offset, sortField, sortOrder, decorate).url);

    return fetch('GET', url).then((response) => {
      const result = jQuery.extend({}, response);

      result.fields = response.fields.map((field) => {
        return {
          hash: md5(field),
          name: field,
          standard_selected: (field === 'message' || field === 'source'),
        };
      });

      result.messages = result.messages.map((message) => MessageFormatter.formatMessageSummary(message));

      return result;
    });
  },
});

UniversalSearchStore.extractTimeRange = (type, timerange) => {
  // The server API uses the `range` parameter instead of `relative` for indicating a relative time range.
  if (type === 'relative') {
    return { range: timerange.range || timerange.relative };
  }

  return timerange;
};

export default UniversalSearchStore;
