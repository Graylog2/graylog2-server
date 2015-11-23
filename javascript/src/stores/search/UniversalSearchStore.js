import Reflux from 'reflux';
import jQuery from 'jquery';
import moment from 'moment';
import md5 from 'md5';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const UniversalSearchStore = Reflux.createStore({
  listenables: [],
  filteredFields: [
    // ElasticSearch fields.
    '_id',
    '_ttl',
    '_source',
    '_all',
    '_index',
    '_type',
    '_score',

    // Our reserved fields.
    'gl2_source_node',
    'gl2_source_input',
    'gl2_source_radio',
    'gl2_source_radio_input',
    'gl2_source_collector',
    'gl2_source_collector_input',
    'gl2_remote_ip',
    'gl2_remote_port',
    'gl2_remote_hostname',
    'streams',
  ],
  _filterFields(fields) {
    const result = {};

    Object.keys(fields).forEach((field) => {
      if (this.filteredFields.indexOf(field) < 0) {
        result[field] = fields[field];
      }
    });

    return result;
  },
  search(type, query, timerange) {
    const parameters = Object.keys(timerange).map((key) => key === 'relative' ? 'range=' + timerange[key] : key + '=' + timerange[key]);
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.UniversalSearchApiController.search(type, query).url + '&' + parameters.join('&'));

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
        const newMessage = {
          id: message._id,
          timestamp: moment(message.timestamp).unix(),
          filtered_fields: this._filterFields(message),
          formatted_fields: this._filterFields(message),
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
  histogram(type, query, timerange, interval) {
    const parameters = Object.keys(timerange).map((key) => key === 'relative' ? 'range=' + timerange[key] : key + '=' + timerange[key]);
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.UniversalSearchApiController.histogram(type, query, interval).url
        + '&' + parameters.join('&'));

    return fetch('GET', url).then((response) => {
      response.histogram_boundaries = response.queried_timerange;
      return response;
    });
  },
});

export default UniversalSearchStore;
