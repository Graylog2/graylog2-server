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
  },

  getInitialState() {
    return {
      pagination: this.pagination,
      errorStates: {
        tables: {},
        caches: {},
        dataAdapters: {},
      },
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
      const lookupTable = response.lookup_tables[0];
      this.trigger({
        table: lookupTable,
        cache: response.caches[lookupTable.cache_id],
        dataAdapter: response.data_adapters[lookupTable.data_adapter_id],
      });
    }, this._errorHandler(`Fetching lookup table ${idOrName} failed`,
      'Could not retrieve lookup table'));

    LookupTablesActions.get.promise(promise);
    return promise;
  },

  create(table) {
    const url = this._url('tables');
    const promise = fetch('POST', url, table);

    promise.catch(this._errorHandler('Creating lookup table failed', `Could not create lookup table "${table.name}"`));

    LookupTablesActions.create.promise(promise);
    return promise;
  },

  update(table) {
    const url = this._url(`tables/${table.id}`);
    const promise = fetch('PUT', url, table);

    promise.catch(this._errorHandler('Updating lookup table failed', `Could not update lookup table "${table.name}"`));

    LookupTablesActions.update.promise(promise);
    return promise;
  },

  delete(idOrName) {
    const url = this._url(`tables/${idOrName}`);
    const promise = fetch('DELETE', url);

    promise.catch(this._errorHandler('Deleting lookup table failed', `Could not delete lookup table "${idOrName}"`));

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
          tables: response.tables || {},
          caches: response.caches || {},
          dataAdapters: response.data_adapters || {},
        },
      });
    }, this._errorHandler('Fetching lookup table error state failed.', 'Could not error states'));

    LookupTablesActions.getErrors.promise(promise);
    return promise;
  },

  lookup(tableName, key) {
    const promise = fetch('GET', this._url(`tables/${tableName}/query?key=${key}`));

    promise.then((response) => {
      this.trigger({
        lookupResult: response,
      });
    }, this._errorHandler('Lookup failed', `Could not lookup value for key "${key}" in lookup table "${tableName}"`));

    LookupTablesActions.lookup.promise(promise);
    return promise;
  },

  validate(table) {
    const url = this._url('tables/validate');
    const promise = fetch('POST', url, table);

    promise.then((response) => {
      this.trigger({
        validationErrors: response.errors,
      });
    }, this._errorHandler('Lookup table validation failed', `Could not validate lookup table "${table.name}"`));
    LookupTablesActions.validate.promise(promise);
    return promise;
  },

  _errorHandler(message, title, cb) {
    return (error) => {
      try {
        // Do not show the user notification if the error is a hibernate error message. We cannot display those
        // properly yet...
        if (error.additional.body[0].message_template) {
          return;
        }
      } catch (e) {
        // ignored
      }
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
