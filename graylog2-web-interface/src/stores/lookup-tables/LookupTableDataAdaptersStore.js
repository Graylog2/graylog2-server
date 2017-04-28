import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';

const LookupTableDataAdaptersActions = ActionsProvider.getActions('LookupTableDataAdapters');

const LookupTableDataAdaptersStore = Reflux.createStore({
  listenables: [LookupTableDataAdaptersActions],

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
      dataAdapters: undefined,
      pagination: this.pagination,
    };
  },

  reloadPage() {
    const promise = this.searchPaginated(this.pagination.page, this.pagination.per_page, this.pagination.query);
    LookupTableDataAdaptersActions.reloadPage.promise(promise);
    return promise;
  },

  searchPaginated(page, perPage, query) {
    let url;
    if (query) {
      url = this._url(`adapters?page=${page}&per_page=${perPage}&query=${encodeURIComponent(query)}`);
    } else {
      url = this._url(`adapters?page=${page}&per_page=${perPage}`);
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
      this.trigger({ pagination: this.pagination, dataAdapters: response.data_adapters });
    }, this._errorHandler('Fetching lookup table data adapters failed', 'Could not retrieve the lookup dataAdapters'));

    LookupTableDataAdaptersActions.searchPaginated.promise(promise);
    return promise;
  },

  get(idOrName) {
    const url = this._url(`adapters/${idOrName}`);
    const promise = fetch('GET', url);

    promise.then((response) => {
      this.trigger({ dataAdapter: response });
    }, this._errorHandler(`Fetching lookup table data adapter ${idOrName} failed`, 'Could not retrieve lookup table data adapter'));

    LookupTableDataAdaptersActions.get.promise(promise);
    return promise;
  },

  create(dataAdapter) {
    const url = this._url('adapters');
    const promise = fetch('POST', url, dataAdapter);

    promise.then((response) => {
      this.trigger({ dataAdapter: response });
    });

    LookupTableDataAdaptersActions.create.promise(promise);
    return promise;
  },

  update(dataAdapter) {
    const url = this._url('adapters');
    const promise = fetch('PUT', url, dataAdapter);

    promise.then((response) => {
      this.trigger({ dataAdapter: response });
    });

    LookupTableDataAdaptersActions.update.promise(promise);
    return promise;
  },

  getTypes() {
    const url = this._url('types/adapters');
    const promise = fetch('GET', url);

    promise.then((response) => {
      this.trigger({ types: response });
    });

    LookupTableDataAdaptersActions.getTypes.promise(promise);
    return promise;
  },

  delete(idOrName) {
    const url = this._url(`adapters/${idOrName}`);
    const promise = fetch('DELETE', url);

    LookupTableDataAdaptersActions.delete.promise(promise);
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

export default LookupTableDataAdaptersStore;
