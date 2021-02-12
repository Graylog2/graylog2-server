/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import * as URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const LookupTableDataAdaptersActions = ActionsProvider.getActions('LookupTableDataAdapters');

const LookupTableDataAdaptersStore = Reflux.createStore({
  listenables: [LookupTableDataAdaptersActions],
  dataAdapter: null,
  dataAdapters: undefined,
  types: null,
  pagination: {
    page: 1,
    per_page: 10,
    total: 0,
    count: 0,
    query: null,
  },
  lookupResult: null,
  validationErrors: {},

  getInitialState() {
    return this.getState();
  },

  getState() {
    return {
      dataAdapter: this.dataAdapter,
      dataAdapters: this.dataAdapters,
      lookupResult: this.lookupResult,
      types: this.types,
      pagination: this.pagination,
      validationErrors: this.validationErrors,
    };
  },

  propagateChanges() {
    this.trigger(this.getState());
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

      this.dataAdapters = response.data_adapters;
      this.propagateChanges();
    }, this._errorHandler('Fetching lookup table data adapters failed', 'Could not retrieve the lookup dataAdapters'));

    LookupTableDataAdaptersActions.searchPaginated.promise(promise);

    return promise;
  },

  get(idOrName) {
    const url = this._url(`adapters/${idOrName}`);
    const promise = fetch('GET', url);

    promise.then((response) => {
      this.dataAdapter = response;
      this.propagateChanges();
    }, this._errorHandler(`Fetching lookup table data adapter ${idOrName} failed`, 'Could not retrieve lookup table data adapter'));

    LookupTableDataAdaptersActions.get.promise(promise);

    return promise;
  },

  create(dataAdapter) {
    const url = this._url('adapters');
    const promise = fetch('POST', url, dataAdapter);

    promise.then((response) => {
      this.dataAdapter = response;
      this.propagateChanges();
    }, this._errorHandler('Creating lookup table data adapter failed', `Could not create lookup table data adapter "${dataAdapter.name}"`));

    LookupTableDataAdaptersActions.create.promise(promise);

    return promise;
  },

  update(dataAdapter) {
    const url = this._url(`adapters/${dataAdapter.id}`);
    const promise = fetch('PUT', url, dataAdapter);

    promise.then((response) => {
      this.dataAdapter = response;
      this.propagateChanges();
    }, this._errorHandler('Updating lookup table data adapter failed', `Could not update lookup table data adapter "${dataAdapter.name}"`));

    LookupTableDataAdaptersActions.update.promise(promise);

    return promise;
  },

  getTypes() {
    const url = this._url('types/adapters');
    const promise = fetch('GET', url);

    promise.then((response) => {
      this.types = response;
      this.propagateChanges();
    }, this._errorHandler('Fetching available types failed', 'Could not fetch the available lookup table data adapter types'));

    LookupTableDataAdaptersActions.getTypes.promise(promise);

    return promise;
  },

  delete(idOrName) {
    const url = this._url(`adapters/${idOrName}`);
    const promise = fetch('DELETE', url);

    promise.catch(this._errorHandler('Deleting lookup table data adapter failed', `Could not delete lookup table data adapter "${idOrName}"`));

    LookupTableDataAdaptersActions.delete.promise(promise);

    return promise;
  },

  lookup(adapterName, key) {
    const promise = fetch('GET', this._url(`adapters/${adapterName}/query?key=${encodeURIComponent(key)}`));

    promise.then((response) => {
      this.lookupResult = response;
      this.propagateChanges();
    }, this._errorHandler('Lookup failed', `Could not lookup value for key "${key}" in lookup table data adapter "${adapterName}"`));

    LookupTableDataAdaptersActions.lookup.promise(promise);

    return promise;
  },

  validate(adapter) {
    const url = this._url('adapters/validate');
    const promise = fetch('POST', url, adapter);

    promise.then((response) => {
      this.validationErrors = response.errors;
      this.propagateChanges();
    }, this._errorHandler('Lookup table data adapter validation failed', `Could not validate lookup table data adapter "${adapter.name}"`));

    LookupTableDataAdaptersActions.validate.promise(promise);

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
