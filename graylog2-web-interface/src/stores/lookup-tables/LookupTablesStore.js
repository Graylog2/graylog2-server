import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';

const LookupTablesActions = ActionsProvider.getActions('LookupTables');

const LookupTablesStore = Reflux.createStore({
  listenables: [LookupTablesActions],

  init() {
    this.pagination = {
      page: 1,
      per_page: 10,
      total: 0,
      count: 0,
      query: null,
    };
    this.errorStates = {
      tables: {},
      caches: {},
      dataAdapters: {},
    };
  },

  getInitialState() {
    return {
      pagination: this.pagination,
      errorStates: this.errorStates,
    };
  },

  reloadPage() {
    const promise = this.searchPaginated(this.pagination.page, this.pagination.per_page,
      this.pagination.query);
    LookupTablesActions.reloadPage.promise(promise);
    return promise;
  },

  searchPaginated(page, perPage, query) {
    let url;
    if (query) {
      url = this._url(
        `tables?page=${page}&per_page=${perPage}&query=${encodeURIComponent(query)}&resolve=true`);
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
      this.trigger({
        tables: response.lookup_tables,
        caches: response.caches,
        dataAdapters: response.data_adapters,
        pagination: this.pagination,
      });
    }, this._errorHandler('Fetching lookup tables failed', 'Could not retrieve the lookup tables'));

    LookupTablesActions.searchPaginated.promise(promise);
    return promise;
  },

  get(idOrName) {
    const url = this._url(`tables/${idOrName}?resolve=true`);
    const promise = fetch('GET', url);

    promise.then((response) => {
      // do not propagate pagination! it will destroy the subsequent overview page's state.
      this.trigger({
        table: response.lookup_tables[0],
        cache: response.caches,
        dataAdapter: response.data_adapters,
      });
    }, this._errorHandler(`Fetching lookup table ${idOrName} failed`,
      'Could not retrieve lookup table'));

    LookupTablesActions.get.promise(promise);
    return promise;
  },

  create(table) {
    const url = this._url('tables');
    const promise = fetch('POST', url, table);

    LookupTablesActions.create.promise(promise);
    return promise;
  },

  update(table) {
    const url = this._url('tables');
    const promise = fetch('PUT', url, table);

    LookupTablesActions.update.promise(promise);
    return promise;
  },

  delete(idOrName) {
    const url = this._url(`tables/${idOrName}`);
    const promise = fetch('DELETE', url);

    LookupTablesActions.delete.promise(promise);
    return promise;
  },

  getErrors(tableNames, cacheNames, dataAdapterNames) {
    const request = {};
    if (tableNames) {
      request.tables = tableNames;
    }
    if (cacheNames) {
      request.caches = cacheNames;
    }
    if (dataAdapterNames) {
      request.data_adapters = dataAdapterNames;
    }

    const promise = fetch('POST', this._url('errorstates'), request);

    promise.then((response) => {
      this.trigger({
        errorStates: {
          tables: response.tables || [],
          caches: response.caches || [],
          dataAdapters: response.data_adapters || [],
        },
      });
    }, this._errorHandler('Fetching lookup table error state failed.', 'Could not error states'));

    LookupTablesActions.getErrors.promise(promise);
    return promise;
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
