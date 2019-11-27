import Reflux from 'reflux';
import Immutable from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import StoreProvider from 'injection/StoreProvider';

const SearchStore = StoreProvider.getStore('Search');

const FieldStatisticsStore = Reflux.createStore({
  FUNCTIONS: Immutable.OrderedMap({
    count: 'Total',
    mean: 'Mean',
    min: 'Minimum',
    max: 'Maximum',
    std_deviation: 'Std. deviation',
    variance: 'Variance',
    sum: 'Sum',
    cardinality: 'Cardinality',
  }),
  getFieldStatistics(field) {
    const originalSearchURLParams = SearchStore.getOriginalSearchURLParams();
    const streamId = SearchStore.searchInStream ? SearchStore.searchInStream.id : null;

    const rangeType = originalSearchURLParams.get('rangetype');
    const timerange = {};
    switch (rangeType) {
      case 'relative':
        timerange.range = originalSearchURLParams.get('relative');
        break;
      case 'absolute':
        timerange.from = originalSearchURLParams.get('from');
        timerange.to = originalSearchURLParams.get('to');
        break;
      case 'keyword':
        timerange.keyword = originalSearchURLParams.get('keyword');
        break;
      default:
        throw new Error('Invalid range type, should be one of "relative", "absolute" or "keyword"');
    }

    let { url } = ApiRoutes.UniversalSearchApiController.fieldStats(
      rangeType,
      originalSearchURLParams.get('q') || '*',
      field,
      timerange,
      streamId,
    );

    url = URLUtils.qualifyUrl(url);

    const promise = fetch('GET', url);
    promise.catch((error) => {
      UserNotification.error(`Loading field statistics failed with status: ${error}`,
        'Could not load field statistics');
    });

    return promise;
  },
});

export default FieldStatisticsStore;
