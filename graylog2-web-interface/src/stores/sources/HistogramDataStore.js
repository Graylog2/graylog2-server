import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import URI from 'urijs';

import HistogramFormatter from 'logic/graphs/HistogramFormatter';
import HistogramDataActions from 'actions/sources/HistogramDataActions';

const HistogramDataStore = Reflux.createStore({
  listenables: [HistogramDataActions],
  sourceUrl: '/search/universal/relative/histogram',
  histogram: undefined,
  DEFAULT_MAX_DATA_POINTS: 4000,

  getInitialState() {
    return {histogram: this.histogram};
  },

  load(range, sourceNames, maxDataPoints) {
    const url = URI(URLUtils.qualifyUrl(this.sourceUrl));
    const urlQuery = {};

    if (typeof sourceNames !== 'undefined' && sourceNames instanceof Array) {
      urlQuery.query = sourceNames.map((source) => 'source:' + source).join(' OR ');
    } else {
      urlQuery.query = '*';
    }

    let interval = 'minute';
    const rangeAsNumber = Number(range);
    if (rangeAsNumber >= 365 * 24 * 60 * 60 || rangeAsNumber === 0) {
      // for years and all interval will be day
      interval = 'day';
    } else if (rangeAsNumber >= 31 * 24 * 60 * 60) {
      // for months interval will be hour
      interval = 'hour';
    }

    urlQuery.range = range;
    urlQuery.interval = interval;

    url.query(urlQuery);

    const promise = fetch('GET', url.toString())
      .then(
        response => {
          const results = response.results;
          response.values = HistogramFormatter.format(results, response.queried_timerange, interval,
            maxDataPoints || this.DEFAULT_MAX_DATA_POINTS, rangeAsNumber === 0);
          return response;
        },
        error => {
          UserNotification.warning(`Loading of histogram data failed with status: ${error}`,
            'Could not load histogram data');
        }
      );

    HistogramDataActions.load.promise(promise);
  },
});

export default HistogramDataStore;
