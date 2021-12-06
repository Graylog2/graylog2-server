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

import { Store } from 'stores/StoreTypes';
import { singletonStore, singletonActions } from 'logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';

type QueryValidationActionsType = RefluxActions<{
  displayValidationErrors: () => Promise<void>,
}>;

const QueryValidationActions: QueryValidationActionsType = singletonActions(
  'views.QueryValidation',
  () => Reflux.createActions({
    displayValidationErrors: {
      asyncResult: true,
    },
  }),
);

const QueryValidationStore: Store<{}> = singletonStore(
  'views.QueryValidation',
  () => Reflux.createStore({
    listenables: [QueryValidationActions],

    displayValidationErrors() {
      const promise = Promise.resolve();
      QueryValidationActions.displayValidationErrors.promise(promise);

      return promise;
    },
  }),
);

export {
  QueryValidationActions,
  QueryValidationStore,
};
