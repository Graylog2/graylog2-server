import Reflux from 'reflux';
import jQuery from 'jquery';
import moment from 'moment';
import md5 from 'md5';

import HistogramFormatter from 'logic/graphs/HistogramFormatter';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';

import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const UniversalSearchStore = Reflux.createStore({
  listenables: [],

  search(type, query, timerange, streamId, limit) {
    const timerangeParams = UniversalSearchStore.extractTimeRange(type, timerange);

    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.UniversalSearchApiController.search(type, query,
      timerangeParams, streamId, limit).url);

    return fetch('GET', url).then((response) => {
      const result = jQuery.extend({}, response);
      result.fields = response.fields.map((field) => {
        return {
          hash: md5(field),
          name: field,
          standard_selected: (field === 'message' || field === 'source'),
        };
      });

      result.messages = result.messages.map((messageSummary) => {
        const message = messageSummary.message;
        const filteredFields = MessageFieldsFilter.filterFields(message);
        const newMessage = {
          id: message._id,
          timestamp: moment(message.timestamp).unix(),
          filtered_fields: filteredFields,
          formatted_fields: filteredFields,
          fields: message,
          index: messageSummary.index,
          source_node_id: message.gl2_source_node,
          source_input_id: message.gl2_source_input,
          stream_ids: message.streams,
          highlight_ranges: messageSummary.highlight_ranges,
        };
        return newMessage;
      });

      return result;
    });
  },
  histogram(type, query, timerange, interval, streamId, maxDataPoints) {
    const timerangeParams = UniversalSearchStore.extractTimeRange(type, timerange);
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.UniversalSearchApiController.histogram(type, query, interval, timerangeParams, streamId).url);

    return fetch('GET', url).then((response) => {
      response.histogram_boundaries = response.queried_timerange;
      response.histogram = HistogramFormatter.format(response.results, response.histogram_boundaries, interval,
        maxDataPoints, type === 'relative' && timerange.relative === 0, null, true);
      return response;
    });
  },
});

UniversalSearchStore.extractTimeRange = (type, timerange) => {
  // The server API uses the `range` parameter instead of `relative` for indicating a relative time range.
  if (type === 'relative') {
    return {range: timerange.relative};
  }

  return timerange;
};

export default UniversalSearchStore;
