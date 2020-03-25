import Reflux from 'reflux';
import jQuery from 'jquery';
import md5 from 'md5';

import MessageFormatter from 'logic/message/MessageFormatter';

import URLUtils from 'util/URLUtils';
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

      result.messages = result.messages.map(message => MessageFormatter.formatMessageSummary(message));

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
