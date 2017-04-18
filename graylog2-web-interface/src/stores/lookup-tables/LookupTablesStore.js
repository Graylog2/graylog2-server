import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';

const LookupTablesActions = ActionsProvider.getActions('LookupTables');

const LookupTablesStore = Reflux.createStore({
  listenables: [LookupTablesActions],

  init() {
    this.tables = [];
    this.caches = {};
    this.dataAdapters = {};
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
      tables: this.tables,
      caches: this.caches,
      dataAdapters: this.dataAdapters,
      pagination: this.pagination,
    };
  },

  searchPaginated(page, perPage, query) {
    let url;
    if (query) {
      url = this._url(`tables?page=${page}&per_page=${perPage}&query=${encodeURIComponent(query)}&resolve=true`);
    } else {
      url = this._url(`tables?page=${page}&per_page=${perPage}&resolve=true`);
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
      this.tables = response.lookup_tables;
      this.caches = response.caches;
      this.dataAdapters = response.data_adapters;
      this.trigger({ tables: this.tables, pagination: this.pagination, caches: this.caches, dataAdapters: this.dataAdapters });
    }, this._errorHandler('Fetching lookup tables failed', 'Could not retrieve the lookup tables'));

    LookupTablesActions.searchPaginated.promise(promise);
  },

  get(idOrName) {
    const url = this._url(`tables/${idOrName}?resolve=true`);
    const promise = fetch('GET', url);

    promise.then((response) => {
      this.pagination = {
        count: response.count,
        total: response.total,
        page: response.page,
        per_page: response.per_page,
        query: response.query,
      };
      this.tables = response.lookup_tables;
      this.caches = response.caches;
      this.dataAdapters = response.data_adapters;
      this.trigger({ tables: this.tables, pagination: this.pagination, caches: this.caches, dataAdapters: this.dataAdapters });
    }, this._errorHandler(`Fetching lookup table ${idOrName} failed`, 'Could not retrieve lookup table'));

    LookupTablesActions.get.promise(promise);
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

export default LookupTablesStore;
