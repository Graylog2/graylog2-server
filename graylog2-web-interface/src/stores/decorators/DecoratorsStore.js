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

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const DecoratorsActions = ActionsProvider.getActions('Decorators');

const DecoratorsStore = Reflux.createStore({
  listenables: [DecoratorsActions],
  state: {},
  getInitialState() {
    return this.state;
  },
  init() {
    DecoratorsActions.available();
    DecoratorsActions.list();
  },
  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.DecoratorsResource.get().url);
    const promise = fetch('GET', url);

    promise.then((response) => {
      this.trigger({ decorators: response });
      this.state.decorators = response;
    });

    DecoratorsActions.list.promise(promise);

    return promise;
  },
  available() {
    const url = URLUtils.qualifyUrl(ApiRoutes.DecoratorsResource.available().url);
    const promise = fetch('GET', url);

    promise.then((response) => {
      this.trigger({ types: response });
      this.state.types = response;
    });

    DecoratorsActions.available.promise(promise);

    return promise;
  },
  create(request) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DecoratorsResource.create().url);
    const promise = fetch('POST', url, request);

    DecoratorsActions.create.promise(promise);

    return promise;
  },
  createCompleted() {
    DecoratorsActions.list();
  },
  remove(decoratorId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DecoratorsResource.remove(decoratorId).url);

    const promise = fetch('DELETE', url);

    DecoratorsActions.remove.promise(promise);

    return promise;
  },
  removeCompleted() {
    DecoratorsActions.list();
  },
  update(decoratorId, request) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DecoratorsResource.update(decoratorId).url);
    const promise = fetch('PUT', url, request);

    DecoratorsActions.update.promise(promise);

    return promise;
  },
  updateCompleted() {
    DecoratorsActions.list();
  },
});

export default DecoratorsStore;
