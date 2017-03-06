import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import URI from 'urijs';

import HistogramFormatter from 'logic/graphs/HistogramFormatter';

import ActionsProvider from 'injection/ActionsProvider';
const HistogramDataActions = ActionsProvider.getActions('HistogramData');

const HistogramDataStore = Reflux.createStore({
  listenables: [HistogramDataActions],
  sourceUrl: '/search/universal/relative/histogram',
  histogram: undefined,

  getInitialState() {
    return { histogram: this.histogram };
  },

  load(range, sourceNames, maxDataPoints) {
    const url = URI(URLUtils.qualifyUrl(this.sourceUrl));
    const urlQuery = {};

    if (typeof sourceNames !== 'undefined' && sourceNames instanceof Array) {
      urlQuery.query = sourceNames.map(source => `source:${source}`).join(' OR ');
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
        (response) => {
          response.histogram = HistogramFormatter.format(response.results, response.queried_timerange, interval,
            maxDataPoints, rangeAsNumber === 0, null);
          return response;
        },
        (error) => {
          UserNotification.warning(`Loading of histogram data failed with status: ${error}`,
            'Could not load histogram data');
        },
      );

    HistogramDataActions.load.promise(promise);
  },
});

export default HistogramDataStore;
