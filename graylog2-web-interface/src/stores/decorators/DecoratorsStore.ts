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

import * as URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';

export type DecoratorSummary = {
  id: string;
  type: string;
  config: Record<string, unknown>;
  stream: string | null;
  category: string | null;
  order: number;
};

type DecoratorsActionsType = {
  available: () => Promise<Record<string, unknown>>;
  create: (request: unknown) => Promise<unknown>;
  list: () => Promise<Array<DecoratorSummary>>;
  remove: (decoratorId: string) => Promise<unknown>;
  update: (decoratorId: string, request: unknown) => Promise<unknown>;
};

export const DecoratorsActions = singletonActions('core.Decorators', () =>
  Reflux.createActions<DecoratorsActionsType>({
    available: { asyncResult: true },
    create: { asyncResult: true },
    list: { asyncResult: true },
    remove: { asyncResult: true },
    update: { asyncResult: true },
  }),
);

export const DecoratorsStore = singletonStore('core.Decorators', () =>
  Reflux.createStore({
    listenables: [DecoratorsActions],
    state: {} as { decorators?: unknown; types?: unknown },
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

      promise.then((response: unknown) => {
        this.trigger({ decorators: response });
        this.state.decorators = response;
      });

      DecoratorsActions.list.promise(promise);

      return promise;
    },
    available() {
      const url = URLUtils.qualifyUrl(ApiRoutes.DecoratorsResource.available().url);
      const promise = fetch('GET', url);

      promise.then((response: unknown) => {
        this.trigger({ types: response });
        this.state.types = response;
      });

      DecoratorsActions.available.promise(promise);

      return promise;
    },
    create(request: unknown) {
      const url = URLUtils.qualifyUrl(ApiRoutes.DecoratorsResource.create().url);
      const promise = fetch('POST', url, request);

      DecoratorsActions.create.promise(promise);

      return promise;
    },
    createCompleted() {
      DecoratorsActions.list();
    },
    remove(decoratorId: string) {
      const url = URLUtils.qualifyUrl(ApiRoutes.DecoratorsResource.remove(decoratorId).url);

      const promise = fetch('DELETE', url);

      DecoratorsActions.remove.promise(promise);

      return promise;
    },
    removeCompleted() {
      DecoratorsActions.list();
    },
    update(decoratorId: string, request: unknown) {
      const url = URLUtils.qualifyUrl(ApiRoutes.DecoratorsResource.update(decoratorId).url);
      const promise = fetch('PUT', url, request);

      DecoratorsActions.update.promise(promise);

      return promise;
    },
    updateCompleted() {
      DecoratorsActions.list();
    },
  }),
);
