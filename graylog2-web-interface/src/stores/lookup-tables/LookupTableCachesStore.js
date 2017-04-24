import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';

const LookupTableCachesActions = ActionsProvider.getActions('LookupTableCaches');

const LookupTableCachesStore = Reflux.createStore({
  listenables: [LookupTableCachesActions],

  init() {
    this.caches = [];
    this.pagination = {
      page: 1,
      per_page: 10,
      total: 0,
      count: 0,
      query: null,
    };
  },

  getInitialState() {
    return {
      caches: this.caches,
      pagination: this.pagination,
    };
  },

  searchPaginated(page, perPage, query) {
    let url;
    if (query) {
      url = this._url(`caches?page=${page}&per_page=${perPage}&query=${encodeURIComponent(query)}`);
    } else {
      url = this._url(`caches?page=${page}&per_page=${perPage}`);
    }
    const promise = fetch('GET', url);

    promise.then((response) => {
      this.pagination = {
        count: response.count,
        total: response.total,
        page: response.page,
        per_page: response.per_page,
        query: response.query,
      };
      this.caches = response.caches;
      this.trigger({ pagination: this.pagination, caches: this.caches });
    }, this._errorHandler('Fetching lookup table caches failed', 'Could not retrieve the lookup caches'));

    LookupTableCachesActions.searchPaginated.promise(promise);
  },

  get(idOrName) {
    const url = this._url(`caches/${idOrName}`);
    const promise = fetch('GET', url);

    promise.then((response) => {
      this.caches = [response];
      this.trigger({ caches: this.caches });
    }, this._errorHandler(`Fetching lookup table cache ${idOrName} failed`, 'Could not retrieve lookup table cache'));

    LookupTableCachesActions.get.promise(promise);
  },

  _errorHandler(message, title, cb) {
    return (error) => {
      let errorMessage;
      try {
        errorMessage = error.additional.body.message;
      } catch (e) {
        errorMessage = error.message;
      }
      UserNotification.error(`${message}: ${errorMessage}`, title);
      if (cb) {
        cb(error);
      }
    };
  },

  _url(path) {
    return URLUtils.qualifyUrl(`/system/lookup/${path}`);
  },
});

export default LookupTableCachesStore;
