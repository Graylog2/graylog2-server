import Reflux from 'reflux';
import URI from 'urijs';
import lodash from 'lodash';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import CombinedProvider from 'injection/CombinedProvider';

const { EventsActions } = CombinedProvider.get('Events');

const EventsStore = Reflux.createStore({
  listenables: [EventsActions],
  sourceUrl: '/plugins/org.graylog.events/events/search',
  events: undefined,
  totalEvents: undefined,
  context: undefined,
  parameters: {
    page: undefined,
    pageSize: undefined,
    query: undefined,
  },

  getInitialState() {
    return this.getState();
  },

  propagateChanges() {
    this.trigger(this.getState());
  },

  getState() {
    return {
      events: this.events,
      parameters: this.parameters,
      totalEvents: this.totalEvents,
      context: this.context,
    };
  },

  eventsUrl({ segments = [], query = {} }) {
    const uri = new URI(this.sourceUrl);
    const nextSegments = lodash.concat(uri.segment(), segments);
    uri.segmentCoded(nextSegments);
    uri.query(query);

    return URLUtils.qualifyUrl(uri.resource());
  },

  refresh() {
    const { query, page, pageSize } = this.parameters;
    this.search({
      query: query,
      page: page,
      pageSize: pageSize,
    });
  },

  search({ query = '', page = 1, pageSize = 10 }) {
    const promise = fetch('POST', this.eventsUrl({}), {
      query: query,
      page: page,
      per_page: pageSize,
    });

    promise.then((response) => {
      this.events = response.events;
      this.parameters = {
        query: response.parameters.query,
        page: response.parameters.page,
        pageSize: response.parameters.per_page,
      };
      this.totalEvents = response.total_events;
      this.context = response.context;
      this.propagateChanges();
      return response;
    });

    EventsActions.search.promise(promise);
  },
});

export default EventsStore;
